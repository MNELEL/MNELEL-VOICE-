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

    fun startRecording(): File? {
        try {
            val cacheDir = context.cacheDir
            val audioFile = File.createTempFile("voice_sample_", ".aac", cacheDir)
            currentRecordingFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            return audioFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to start recording", e)
            return null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun pauseRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to pause recording", e)
        }
    }

    fun resumeRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to resume recording", e)
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to stop recording", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun playAudio(file: File, onComplete: () -> Unit = {}) {
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    onComplete()
                    stopPlayback()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to play audio file", e)
        }
    }

    fun playBase64Audio(base64Data: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = File.createTempFile("synth_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            playAudio(tempFile, onComplete)
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to play base64 audio", e)
        }
    }

    fun saveBase64ToPersistentFile(base64Data: String, fileNamePrefix: String): File? {
        return try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val outputDir = File(context.filesDir, "cloned_voices")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val cleanPrefix = fileNamePrefix.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            val formattedPrefix = if (cleanPrefix.isBlank()) "cloned" else cleanPrefix
            val targetFile = File(outputDir, "${formattedPrefix}_${System.currentTimeMillis()}.mp3")
            FileOutputStream(targetFile).use { fos ->
                fos.write(audioBytes)
            }
            targetFile
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to save base64 to persistent file", e)
            null
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

    fun isPlaybackPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to stop playback", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun fileToBase64(file: File): String? {
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to convert file to base64" + e.message, e)
            null
        }
    }
}
