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
