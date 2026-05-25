package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File

class AudioHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null

    fun startRecording(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            stopRecording() // Clean any previous recorder active
            stopPlayback()  // Stop any active playbacks

            val audioFile = File(context.cacheDir, "voice_record_${System.currentTimeMillis()}.3gp")
            currentFile = audioFile

            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("AudioHelper", "MediaRecorder error: what=$what, extra=$extra")
                }
                setOnInfoListener { _, what, extra ->
                    android.util.Log.i("AudioHelper", "MediaRecorder info: what=$what, extra=$extra")
                }
                prepare()
                start()
            }
            onSuccess(audioFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }

    fun stopRecording(): String? {
        mediaRecorder?.let { recorder ->
            try {
                recorder.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                recorder.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        return currentFile?.absolutePath
    }

    fun playAudio(path: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        try {
            stopPlayback()
            val file = File(path)
            if (!file.exists()) {
                throw Exception("Audio file not found at: $path")
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    Handler(Looper.getMainLooper()).post {
                        onComplete()
                        stopPlayback()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Handler(Looper.getMainLooper()).post {
                        onError(Exception("MediaPlayer playback error: what=$what, extra=$extra"))
                        stopPlayback()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }

    fun stopPlayback() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
    }
}

