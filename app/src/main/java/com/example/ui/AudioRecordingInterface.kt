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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
    durationMs: Long = 0L,
    amplitude: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    liveDecibels: Float = 20f,
    isNoiseMonitoring: Boolean = false,
    onToggleNoiseMonitoring: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showGuidelinesDialog by remember { mutableStateOf(false) }

    if (showGuidelinesDialog) {
        AlertDialog(
            onDismissRequest = { showGuidelinesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("איך להקליט דגימה אופטימלית?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• הקלט בחדר שקט ללא רעשי רקע (מזגן, כביש, מאוורר).")
                    Text("• דבר בצורה ברורה ובקצב טבעי.")
                    Text("• שמור על מרחק אחיד מהמיקרופון (כ-15 ס\"מ).")
                    Text("• הימנע מהדהוד (כגון חדרים ריקים גדולים).")
                    Text("• מומלץ להקליט לפחות 15-30 שניות לקבלת פרופיל קול מדויק ויציב.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuidelinesDialog = false }) {
                    Text("הבנתי")
                }
            }
        )
    }

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
            // Live Status Header with Info Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Info Button
                IconButton(
                    onClick = { showGuidelinesDialog = true },
                    modifier = Modifier.align(Alignment.CenterStart).size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "הנחיות הקלטה", 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                // Live Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.Center)
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
        }

            AnimatedVisibility(
                visible = liveDecibels > 60f && (isRecording || isNoiseMonitoring),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "אזהרה",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "אזהרה: סביבה רועשת מדי",
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "רמת הרעש גבוהה ועלולה לפגוע בדיוק פרופיל הקול. אנא עבור לחדר שקט.",
                                color = Color(0xFFD32F2F).copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Decibel Meter Component
            DecibelMeterComponent(
                decibels = liveDecibels,
                isMonitoring = isNoiseMonitoring,
                isRecording = isRecording,
                onToggleMonitor = onToggleNoiseMonitoring,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Audio Wave Visualizer Area
            RecordingWaveform(
                isRecording = isRecording || isNoiseMonitoring,
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
                // Precise Duration Counter Block
                val min = (durationMs / 60000) % 60
                val sec = (durationMs / 1000) % 60
                val centiseconds = (durationMs / 10) % 100
                val timeFormatted = String.format("%02d:%02d.%02d", min, sec, centiseconds)

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
            FrequencyVisualizer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                isActive = isRecording && !isPaused,
                amplitude = amplitude,
                barCount = 40,
                barColor = MaterialTheme.colorScheme.primary,
                bottomAligned = false
            )
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

@Composable
fun DecibelMeterComponent(
    decibels: Float,
    isMonitoring: Boolean,
    isRecording: Boolean,
    onToggleMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedDb by animateFloatAsState(
        targetValue = decibels,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "decibels"
    )

    val (color, statusText, description) = when {
        animatedDb < 45f -> Triple(
            Color(0xFF10B981), // Emerald green
            "שקט מצוין",
            "סביבה מעולה להקלטה נקייה מרעשים"
        )
        animatedDb < 60f -> Triple(
            Color(0xFFF59E0B), // Amber yellow
            "רעש רקע מתון",
            "מומלץ לסגור חלונות או לעבור לחדר שקט"
        )
        else -> Triple(
            Color(0xFFEF4444), // Crimson red
            "רועש מדי ⚠️",
            "רעש רקע חזק עלול לפגוע באיכות השיבוט"
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "מד דציבלים ורעש סביבתי",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isRecording) {
                    Button(
                        onClick = onToggleMonitor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isMonitoring) "הפסק בדיקה" else "בדיקת רעש רקע",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "מדידה פעילה בהקלטה",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Level Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                val dbFraction = ((animatedDb - 20f) / 70f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(dbFraction)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF10B981),
                                    Color(0xFFF59E0B),
                                    Color(0xFFEF4444)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom detail text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${animatedDb.toInt()} dB - $statusText",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = color
                )

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
        }
    }
}
