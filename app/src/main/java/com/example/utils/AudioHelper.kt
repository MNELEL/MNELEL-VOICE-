package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    // --- Recording ---

    fun startRecording(): File? {
        try {
            stopRecording()
            
            // Create a temporary file in the cache directory
            val outputDir = context.cacheDir
            val outputFile = File.createTempFile("recording_", ".mp4", outputDir)
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val attributedContext = context.createAttributionContext("VoiceClonerTag")
                MediaRecorder(attributedContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            Log.d("AudioHelper", "Recording started: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            return null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                Log.d("AudioHelper", "Recording paused")
            } catch (e: Exception) {
                Log.e("AudioHelper", "Failed to pause recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                Log.d("AudioHelper", "Recording resumed")
            } catch (e: Exception) {
                Log.e("AudioHelper", "Failed to resume recording", e)
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error stopping transmitter / recorder", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private var presetReverb: android.media.audiofx.PresetReverb? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    var activeAcousticPreset: String = "None" // "None", "Studio", "Room", "Hall", "Cathedral", "Radio", "Podcast", "Echo"

    // --- Playback ---

    fun playAudio(file: File, playbackSpeed: Float = 1.0f, onCompletion: () -> Unit) {
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(playbackSpeed)
                    } catch (e: Exception) {
                        Log.e("AudioHelper", "Failed to set playback speed", e)
                    }
                }
                
                if (activeAcousticPreset != "None") {
                    try {
                        val isReverb = activeAcousticPreset in listOf("Studio", "Room", "Hall", "Cathedral", "Echo")
                        val isEq = activeAcousticPreset in listOf("Radio", "Podcast")
                        
                        if (isReverb) {
                            val presetVal: Short = when (activeAcousticPreset) {
                                "Studio" -> android.media.audiofx.PresetReverb.PRESET_SMALLROOM
                                "Room" -> android.media.audiofx.PresetReverb.PRESET_LARGEROOM
                                "Hall" -> android.media.audiofx.PresetReverb.PRESET_LARGEHALL
                                "Cathedral" -> android.media.audiofx.PresetReverb.PRESET_PLATE
                                "Echo" -> android.media.audiofx.PresetReverb.PRESET_LARGEHALL // Echo mapped to Large Hall
                                else -> 0
                            }
                            if (presetVal > 0) {
                                presetReverb?.release()
                                presetReverb = android.media.audiofx.PresetReverb(1, audioSessionId).apply {
                                    preset = presetVal
                                    enabled = true
                                }
                                attachAuxEffect(presetReverb!!.id)
                                setAuxEffectSendLevel(1.0f)
                            }
                        }
                        
                        if (isEq) {
                            equalizer?.release()
                            equalizer = android.media.audiofx.Equalizer(1, audioSessionId).apply {
                                enabled = true
                                val numBands = numberOfBands
                                if (activeAcousticPreset == "Radio") {
                                    // Radio effect: Boost mids, cut lows and highs
                                    for (i in 0 until numBands) {
                                        val freq = getCenterFreq(i.toShort())
                                        if (freq < 400000 || freq > 4000000) {
                                            setBandLevel(i.toShort(), -1500) // Cut lows/highs
                                        } else {
                                            setBandLevel(i.toShort(), 800) // Boost mids
                                        }
                                    }
                                } else if (activeAcousticPreset == "Podcast") {
                                    // Podcast effect: Boost lows for depth, boost presence
                                    for (i in 0 until numBands) {
                                        val freq = getCenterFreq(i.toShort())
                                        if (freq < 200000) {
                                            setBandLevel(i.toShort(), 500) // Boost bass
                                        } else if (freq > 3000000 && freq < 6000000) {
                                            setBandLevel(i.toShort(), 400) // Boost presence
                                        } else {
                                            setBandLevel(i.toShort(), 0) // Flat mids
                                        }
                                    }
                                }
                            }
                        }
                        
                        Log.d("AudioHelper", "Applied Acoustic Preset: $activeAcousticPreset to session $audioSessionId")
                    } catch (e: Exception) {
                        Log.e("AudioHelper", "Failed to apply Acoustic Preset", e)
                    }
                }

                start()
                setOnCompletionListener {
                    onCompletion()
                    stopPlayback()
                }
            }
            Log.d("AudioHelper", "Playing: ${file.absolutePath} at ${playbackSpeed}x")
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to play audio file", e)
            onCompletion()
        }
    }

    fun stopPlayback() {
        try {
            presetReverb?.apply {
                try {
                    enabled = false
                    release()
                } catch (e: Exception) {
                    Log.e("AudioHelper", "Failed to disable presetReverb", e)
                }
            }
            presetReverb = null
            equalizer?.apply {
                try {
                    enabled = false
                    release()
                } catch (e: Exception) {
                    Log.e("AudioHelper", "Failed to disable equalizer", e)
                }
            }
            equalizer = null
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun isPlaybackPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getPlaybackPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getPlaybackDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to seek", e)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.apply {
                    val wasPlaying = isPlaying
                    playbackParams = playbackParams.setSpeed(speed)
                    if (!wasPlaying) {
                        pause() // setting speed can auto start in some versions, so ensure paused status if not originally playing
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to set playback speed: $speed", e)
        }
    }

    fun setPlaybackPitch(pitch: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.apply {
                    val wasPlaying = isPlaying
                    playbackParams = playbackParams.setPitch(pitch)
                    if (!wasPlaying) {
                        pause()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to set playback pitch: $pitch", e)
        }
    }

    // --- Base64 & Persistence Helpers ---

    fun saveFileToPersistentStorage(file: File, prefix: String): File? {
        return try {
            val persistentDir = File(context.filesDir, "saved_recordings")
            if (!persistentDir.exists()) {
                persistentDir.mkdirs()
            }
            val sanitizedPrefix = prefix.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val persistentFile = File(persistentDir, "record_${sanitizedPrefix}_${System.currentTimeMillis()}.mp4")
            file.inputStream().use { input ->
                persistentFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("AudioHelper", "Saved recorded audio to persistent storage: ${persistentFile.absolutePath}")
            persistentFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to save file to persistent storage", e)
            null
        }
    }

    fun fileToBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to convert file to base64", e)
            ""
        }
    }

    fun saveBase64ToPersistentFile(base64: String, prefix: String): File? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val persistentDir = File(context.filesDir, "cloned_voices")
            if (!persistentDir.exists()) {
                persistentDir.mkdirs()
            }
            val sanitizedPrefix = prefix.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val persistentFile = File(persistentDir, "gen_${sanitizedPrefix}_${System.currentTimeMillis()}.mp3")
            FileOutputStream(persistentFile).use { fos ->
                fos.write(bytes)
            }
            Log.d("AudioHelper", "Saved base64 audio to: ${persistentFile.absolutePath}")
            persistentFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to save base64 to file", e)
            null
        }
    }

    fun playBase64Audio(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File.createTempFile("temp_base64_play_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
            }
            playAudio(tempFile) {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to play base64 audio directly", e)
        }
    }
}
