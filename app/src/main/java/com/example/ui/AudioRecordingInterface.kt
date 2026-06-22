package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.glassmorphic
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.LightGreen

/**
 * A highly clean and polished audio recording interface component for Jetpack Compose.
 * Adheres to Material 3 guidelines and Hebrew RTL visual alignment.
 */
@Composable
fun AudioRecordingInterface(
    isRecording: Boolean,
    isPaused: Boolean,
    durationSec: Int,
    amplitude: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassmorphic(shape = RoundedCornerShape(24.dp), elevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (isRecording) {
                    val pulseAlpha by rememberInfiniteTransition().animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) Color(0xFFF59E0B) 
                                else Color(0xFFEF4444).copy(alpha = pulseAlpha)
                            )
                    )
                    Text(
                        text = if (isPaused) "ההקלטה מושהית" else "מקליט כעת...",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) Color(0xFFD97706) else Color(0xFFEF4444)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )
                    Text(
                        text = "מוכן להקלטה",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }

            // Audio Wave Visualizer Area
            RecordingWaveform(
                isRecording = isRecording,
                isPaused = isPaused,
                amplitude = amplitude
            )

            if (isRecording && !isPaused) {
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedVisibility(
                    visible = amplitude > 0.75f,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEbee))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "התראה: עוצמת שמע או רעש רקע חזקים מדי (עלול לפגוע בשיבוט)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                AnimatedVisibility(
                    visible = amplitude <= 0.75f,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "איכות הקלטה (סביבה שקטה): מעולה",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isRecording) {
                // Duration Counter Block
                val minutes = durationSec / 60
                val seconds = durationSec % 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Required progression bar
                val progress = (durationSec.toFloat() / 15f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (progress >= 1f) LightGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // User Feedback / Requirements Text
                Text(
                    text = if (durationSec < 5) {
                        "נדרש להקליט לפחות 5 שניות (נותרו עוד ${5 - durationSec} שניות) 🎙️"
                    } else if (durationSec < 15) {
                        "משך ההקלטה תקין להתחלת שיבוט! מומלץ להמשיך ל-15 שניות ✨"
                    } else {
                        "יש מספיק מידע קול לשיבוט איכותי ביותר! 🎉"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (durationSec < 5) MaterialTheme.colorScheme.error else LightGreen,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Beautifully designed interactive controllers
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause / Resume Control
                    IconButton(
                        onClick = {
                            if (isPaused) onResume() else onPause()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape)
                    ) {
                        if (isPaused) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "המשך הקלטה",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Box(modifier = Modifier.width(4.dp).height(16.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                Box(modifier = Modifier.width(4.dp).height(16.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                            }
                        }
                    }

                    // Stop & Save Control
                    val canStop = durationSec >= 5
                    IconButton(
                        onClick = onStop,
                        enabled = canStop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (canStop) MaterialTheme.colorScheme.secondary 
                                else Color.Gray.copy(alpha = 0.35f)
                            )
                    ) {
                        if (canStop) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "סיום ושמירה",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Text(
                                text = "${5 - durationSec}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            } else {
                // Large attractive recording button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onStart,
                        modifier = Modifier
                            .size(80.dp)
                            .testTag("mic_button")
                            .clip(CircleShape)
                            .background(BrandNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "התחל הקלטה",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "לחץ להתחלת הקלטת קול",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Custom Sound Wave graphics representation with rolling feedback and real-time input quality indicators.
 */
@Composable
private fun RecordingWaveform(
    isRecording: Boolean,
    isPaused: Boolean,
    amplitude: Float
) {
    val amplitudeHistory = remember { mutableStateListOf<Float>() }

    // Aggregate frames in real time during active recording
    LaunchedEffect(amplitude, isRecording, isPaused) {
        if (isRecording) {
            if (!isPaused) {
                amplitudeHistory.add(amplitude)
                if (amplitudeHistory.size > 50) {
                    amplitudeHistory.removeAt(0)
                }
            }
        } else {
            amplitudeHistory.clear()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.015f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw D3-style coordinate grids (dB thresholds)
                val dbLevels = listOf(0.15f, 0.5f, 0.85f)
                dbLevels.forEach { fraction ->
                    val y = canvasHeight * fraction
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val barWidth = 3.dp.toPx()
                val gap = 2.dp.toPx()
                val step = barWidth + gap
                val maxBars = (canvasWidth / step).toInt()

                val displayList = if (isRecording) {
                    if (amplitudeHistory.isEmpty()) {
                        List(maxBars) { 0.03f }
                    } else {
                        val paddingSize = (maxBars - amplitudeHistory.size).coerceAtLeast(0)
                        val padded = List(paddingSize) { 0.03f } + amplitudeHistory
                        padded.takeLast(maxBars)
                    }
                } else {
                    // pre-recording steady idle flow
                    List(maxBars) { index ->
                        val animOffset = index * 0.15f
                        0.03f + 0.05f * kotlin.math.sin(animOffset).toFloat().coerceAtLeast(0f)
                    }
                }

                val centerY = canvasHeight / 2f
                val startX = (canvasWidth - (displayList.size * step)) / 2f

                val upperPath = Path()
                val lowerPath = Path()

                displayList.forEachIndexed { index, amp ->
                    val x = startX + index * step
                    val maxClipHeight = canvasHeight * 0.85f
                    val barHeight = (amp * maxClipHeight).coerceIn(4.dp.toPx(), maxClipHeight)
                    val top = centerY - barHeight / 2f
                    val bottom = centerY + barHeight / 2f

                    if (index == 0) {
                        upperPath.moveTo(x, top)
                        lowerPath.moveTo(x, bottom)
                    } else {
                        upperPath.lineTo(x, top)
                        lowerPath.lineTo(x, bottom)
                    }

                    val barColor = if (isRecording) {
                        if (isPaused) {
                            Color(0xFFF59E0B)
                        } else {
                            when {
                                amp > 0.75f -> Color(0xFFEF4444)
                                amp < 0.03f -> primaryColor.copy(alpha = 0.35f)
                                else -> primaryColor
                            }
                        }
                    } else {
                        onSurfaceColor.copy(alpha = 0.12f)
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }

                // Stroke D3 outer contour
                if (displayList.isNotEmpty()) {
                    drawPath(
                        path = upperPath,
                        color = secondaryColor.copy(alpha = 0.4f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = lowerPath,
                        color = secondaryColor.copy(alpha = 0.4f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Live input feedback legends
        AnimatedVisibility(
            visible = isRecording && !isPaused,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Text("רועש מדי (עיוות קול) ⚠️", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Text("איכות קלט מעולה ✨", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))
                    Text("שקט / עוצמה נמוכה 🤫", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
