package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceClonerViewModel
import com.example.data.SpeechDiagnosisReport
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiagnosticComparisonScreen(viewModel: VoiceClonerViewModel) {
    val allReports by viewModel.allDiagnosisReports.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "השוואת נתוני אבחון קולי 📊",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "השוואה והדמיה בין דוחות העבר, מאפייני אישיות, ומדדים פסיכו-אקוסטיים (מבוסס D3/Recharts style).",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (allReports.size < 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("נדרשים לפחות שני דוחות אבחון שנשמרו כדי להציג השוואה.", fontSize = 14.sp)
                    }
                }
            }
        } else {
            item {
                // Show comparative radar/bar chart
                RadarComparativeChart(reports = allReports.take(2))
            }

            item {
                Text("פירוט הדוחות האחרונים:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(allReports) { report ->
                ReportSummaryCard(report)
            }
        }
    }
}

@Composable
fun RadarComparativeChart(reports: List<SpeechDiagnosisReport>) {
    if (reports.size != 2) return
    val report1 = reports[0]
    val report2 = reports[1]

    val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981)) // Blue vs Green

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "השוואת מדדים רגשיים ואקוסטיים",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Custom "Recharts"-style Bar Chart comparison
            val metrics = listOf(
                "עייפות" to Pair(report1.fatigueScore, report2.fatigueScore),
                "בהירות" to Pair(report1.clarityScore, report2.clarityScore),
                "הגייה" to Pair(report1.pronunciationClarity, report2.pronunciationClarity),
                "ניגון" to Pair(report1.intonationScore, report2.intonationScore)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val barGroupWidth = w / metrics.size
                    val maxVal = 100f
                    
                    metrics.forEachIndexed { i, metric ->
                        val (name, values) = metric
                        val groupX = i * barGroupWidth
                        
                        // Draw Grid Line
                        drawLine(
                            color = Color.LightGray.copy(alpha=0.5f),
                            start = Offset(0f, h/2),
                            end = Offset(w, h/2),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.LightGray.copy(alpha=0.5f),
                            start = Offset(0f, 0f),
                            end = Offset(w, 0f),
                            strokeWidth = 1f
                        )

                        // Data bars
                        val v1h = (values.first / maxVal) * h
                        val v2h = (values.second / maxVal) * h

                        val barWidth = 16.dp.toPx()
                        val spacing = 4.dp.toPx()

                        val centerX = groupX + (barGroupWidth / 2)
                        
                        // Bar 1 (Report 1)
                        drawRoundRect(
                            color = colors[0],
                            topLeft = Offset(centerX - barWidth - spacing/2, h - v1h),
                            size = Size(barWidth, v1h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Bar 2 (Report 2)
                        drawRoundRect(
                            color = colors[1],
                            topLeft = Offset(centerX + spacing/2, h - v2h),
                            size = Size(barWidth, v2h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                LegendIndicator(report1.labelText, colors[0])
                LegendIndicator(report2.labelText, colors[1])
            }
        }
    }
}

@Composable
fun LegendIndicator(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ReportSummaryCard(report: SpeechDiagnosisReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(report.labelText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("מזג: ${report.emotionalTemperament} | עייפות: ${report.fatigueScore}%", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
