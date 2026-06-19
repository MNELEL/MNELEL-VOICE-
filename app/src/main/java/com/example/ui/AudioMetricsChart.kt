package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VoiceProfile

@Composable
fun AudioMetricsChart(
    profile: VoiceProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F3F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "דיאגרמת תדרים וסטיית גובה קול (Pitch Contour)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF162544)
            )
            Text(
                text = "${profile.frequencyHz} Hz ממוצע",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visualizer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // 1. Draw grid lines (horizontal helper dashed lines)
                val gridLines = 3
                for (i in 1..gridLines) {
                    val y = height * (i.toFloat() / (gridLines + 1))
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Plot pitch contour (smooth wave based on frequency and intonation score)
                // Let's generate a beautiful mathematical wave simulating actual pitch tracking
                val points = 30
                val path = Path()
                val fillPath = Path()

                val baseFreqOffset = (profile.frequencyHz - 100).toFloat().coerceIn(0f, 100f)
                val intonationMod = profile.intonationScore.toFloat() / 100f
                val distortionMod = profile.distortionLevel.toFloat() / 100f

                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width
                    // Mathematically formulate a nice flowing wave that represents the voice profile traits
                    val wave1 = kotlin.math.sin(i.toFloat() * 0.4f) * 20f * intonationMod
                    val wave2 = kotlin.math.cos(i.toFloat() * 0.25f) * 12f * (1f - distortionMod)
                    val noise = if (i % 3 == 0) distortionMod * 5f else 0f
                    
                    // Center around appropriate vertical position based on frequencyHz (Soprano higher, Bass lower)
                    val verticalCenter = height * 0.5f - (baseFreqOffset / 100f * 20f)
                    val y = (verticalCenter + wave1 + wave2 + noise).coerceIn(10f, height - 10f)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw pitch contour gradient fill underneath the line
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.15f),
                            Color(0xFF6366F1).copy(alpha = 0.0f)
                        )
                    )
                )

                // Draw active pitch contour line
                drawPath(
                    path = path,
                    color = Color(0xFF6366F1),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Draw column bars on the right side depicting analysis metrics (clarity, intonation, noise)
                // Just to give it additional richness and dynamic feel
                val barCount = 4
                val barWidth = 12.dp.toPx()
                val spacing = 8.dp.toPx()
                val totalBarsWidth = (barWidth + spacing) * barCount
                val startX = width - totalBarsWidth - 4.dp.toPx()

                val metrics = listOf(
                    profile.clarityScore.toFloat() / 100f,
                    profile.pronunciationClarity.toFloat() / 100f,
                    profile.intonationScore.toFloat() / 100f,
                    (100 - profile.distortionLevel).toFloat() / 100f // Signal purity
                )

                val barColors = listOf(
                    Color(0xFF10B981), // Green
                    Color(0xFF3B82F6), // Blue
                    Color(0xFFF59E0B), // Orange
                    Color(0xFFEC4899)  // Pink
                )

                metrics.forEachIndexed { index, ratio ->
                    val barX = startX + index * (barWidth + spacing)
                    val barHeight = height * ratio * 0.7f // max 70% height
                    val barY = height - barHeight - 4.dp.toPx()

                    drawRoundRect(
                        color = barColors[index].copy(alpha = 0.85f),
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart Legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(label = "קו גובה קול (Pitch)", color = Color(0xFF6366F1))
            LegendItem(label = "חיתוך קולי", color = Color(0xFF10B981))
            LegendItem(label = "מנגינת דיבור", color = Color(0xFFF59E0B))
            LegendItem(label = "ניקיון מרעש", color = Color(0xFFEC4899))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569)
        )
    }
}
