package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioMetricsChart(
    frequencyHz: Int,
    pitchValue: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(200.dp).width(150.dp)) {
        val barWidth = size.width / 3
        val maxVal = 1000f // Scaling factor

        // Frequency Bar
        drawRect(
            color = Color.Blue,
            topLeft = Offset(barWidth / 2, size.height - (frequencyHz / maxVal * size.height)),
            size = Size(barWidth, (frequencyHz / maxVal * size.height))
        )

        // Pitch Bar
        drawRect(
            color = Color.Red,
            topLeft = Offset(barWidth * 1.5f + barWidth / 2, size.height - (pitchValue / maxVal * size.height)),
            size = Size(barWidth, (pitchValue / maxVal * size.height))
        )
    }
}
