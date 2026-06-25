package com.example.utils

import java.io.File
import kotlin.math.sqrt

/**
 * Lightweight local heuristic analyzer that processes audio byte streams
 * to extract basic phonetic features without relying on external APIs.
 * This acts as our "small model" heuristic fallback.
 */
object LocalPhoneticAnalyzer {

    data class PhoneticMetrics(
        val estimatedPitchHz: Int,
        val energyLevel: Int,
        val speechSegmentsCount: Int,
        val clarityEstimate: Int
    )

    fun analyzeAudioFile(file: File): PhoneticMetrics {
        if (!file.exists()) {
            return PhoneticMetrics(120, 50, 1, 50)
        }

        try {
            val bytes = file.readBytes()
            if (bytes.isEmpty()) {
                return PhoneticMetrics(120, 50, 1, 50)
            }

            // Preprocessing: Simple noise reduction (filter out low energy)
            val threshold = 10.0f
            val cleanedBytes = bytes.map { if (Math.abs(it.toFloat()) < threshold) 0.toByte() else it }.toByteArray()
            
            // Heuristic 1: Energy Level Calculation (RMS approximation)
            var sumSquares = 0.0
            var zeroCrossings = 0
            var lastSign = 0
            
            // Process a subset of bytes for fast execution on mobile
            val step = 100
            val samplesCount = cleanedBytes.size / step
            
            for (i in cleanedBytes.indices step step) {
                // Approximate raw amplitude from compressed bytes
                val sample = cleanedBytes[i].toFloat()
                sumSquares += sample * sample
                
                val sign = if (sample > 0) 1 else if (sample < 0) -1 else 0
                if (sign != 0 && lastSign != 0 && sign != lastSign) {
                    zeroCrossings++
                }
                if (sign != 0) {
                    lastSign = sign
                }
            }

            val rms = sqrt(sumSquares / samplesCount.coerceAtLeast(1))
            val energyLevel = (rms * 100 / 128.0).toInt().coerceIn(10, 100)
            
            // Heuristic 2: Zero Crossing Rate (ZCR) mapped to estimated Pitch (Hz)
            val zcr = zeroCrossings.toFloat() / samplesCount.coerceAtLeast(1)
            val estimatedPitchHz = (80 + zcr * 1000).toInt().coerceIn(85, 255)
            
            // Heuristic 3: Estimated speech segments (based on file size/duration bursts)
            // Rough estimation for AAC audio ~32kbps
            val fileDurationSecs = bytes.size / 4000 
            val speechSegmentsCount = (fileDurationSecs / 3).coerceAtLeast(1)
            
            // Heuristic 4: Clarity Estimate based on energy distribution
            val clarityEstimate = (60 + (energyLevel * 0.4)).toInt().coerceIn(50, 95)

            return PhoneticMetrics(
                estimatedPitchHz = estimatedPitchHz,
                energyLevel = energyLevel,
                speechSegmentsCount = speechSegmentsCount,
                clarityEstimate = clarityEstimate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return PhoneticMetrics(140, 60, 2, 70)
        }
    }
}
