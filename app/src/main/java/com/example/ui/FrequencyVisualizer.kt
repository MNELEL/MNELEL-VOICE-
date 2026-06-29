package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * A native Android equivalent to a Web Audio API frequency visualizer.
 * Displays frequency bars using Jetpack Compose Canvas.
 */
@Composable
fun FrequencyVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    amplitude: Float,
    barCount: Int = 30,
    barColor: Color = MaterialTheme.colorScheme.primary,
    bottomAligned: Boolean = false
) {
    // Generate pseudo-frequency data based on the single amplitude value to create an equalizer effect
    val animatedAmplitudes = remember { mutableStateListOf<Float>().apply { 
        repeat(barCount) { add(0.05f) } 
    } }
    
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(isActive, amplitude, phase) {
        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            if (isActive) {
                // Modulate the amplitude to create a frequency-like spread
                val freqModulation = sin(phase * (3f + normalizedIndex * 5f) + normalizedIndex * 10f)
                val targetAmp = 0.05f + amplitude * (0.2f + 0.8f * Math.abs(freqModulation))
                
                // Smoothly interpolate
                animatedAmplitudes[i] = animatedAmplitudes[i] * 0.5f + targetAmp * 0.5f
            } else {
                animatedAmplitudes[i] = animatedAmplitudes[i] * 0.8f + 0.05f * 0.2f
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gap = 3.dp.toPx()
            val totalGapWidth = gap * (barCount - 1)
            val barWidth = ((canvasWidth - totalGapWidth) / barCount).coerceAtLeast(1.dp.toPx())
            
            val startX = (canvasWidth - (barWidth * barCount + totalGapWidth)) / 2f

            for (i in 0 until barCount) {
                val amp = animatedAmplitudes[i].coerceIn(0.05f, 1f)
                val barHeight = (amp * canvasHeight).coerceIn(4.dp.toPx(), canvasHeight)
                
                val x = startX + i * (barWidth + gap)
                val y = if (bottomAligned) {
                    canvasHeight - barHeight
                } else {
                    canvasHeight / 2f - barHeight / 2f
                }
                
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
