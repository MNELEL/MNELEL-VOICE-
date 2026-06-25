package com.example.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlin.math.sin

@Composable
fun VicoLineChartImpl(frequencyHz: Int, intonationScore: Int, modifier: Modifier = Modifier) {
    val entries = remember(frequencyHz, intonationScore) {
        val intonationFactor = intonationScore.toFloat() / 100f
        List(40) { i ->
            val progressHz = frequencyHz.toFloat() + (sin(i.toFloat() * 0.2f) * 15f * intonationFactor)
            FloatEntry(i.toFloat(), progressHz)
        }
    }
    Chart(
        chart = lineChart(),
        model = entryModelOf(entries),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = modifier.fillMaxWidth().height(180.dp)
    )
}

@Composable
fun VicoColumnChartImpl(
    pronunciationClarity: Int,
    clarityScore: Int,
    intonationScore: Int,
    breathPauseScore: Int,
    distortionLevel: Int,
    modifier: Modifier = Modifier
) {
    val entries = remember(pronunciationClarity, clarityScore, intonationScore, breathPauseScore, distortionLevel) {
        listOf(
            FloatEntry(0f, pronunciationClarity.toFloat()),
            FloatEntry(1f, clarityScore.toFloat()),
            FloatEntry(2f, intonationScore.toFloat()),
            FloatEntry(3f, breathPauseScore.toFloat()),
            FloatEntry(4f, (100 - distortionLevel).coerceIn(0, 100).toFloat())
        )
    }
    Chart(
        chart = columnChart(),
        model = entryModelOf(entries),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = modifier.fillMaxWidth().height(180.dp)
    )
}
