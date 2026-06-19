package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
 * Custom Sound Wave graphics representation specifically styled for the clean recording view.
 */
@Composable
private fun RecordingWaveform(
    isRecording: Boolean,
    isPaused: Boolean,
    amplitude: Float
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveBarsCount = 15
    
    val pulseHeights = List(waveBarsCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 12f,
            targetValue = if (isRecording && !isPaused) {
                val ampFactor = if (amplitude > 0.05f) amplitude * 90f else 16f
                val distanceCenter = index - (waveBarsCount / 2)
                val gaussianFactor = Math.max(0.1f, 1f - (distanceCenter * distanceCenter * 0.1f).toFloat())
                (12f + (ampFactor * gaussianFactor)).coerceIn(10f, 85f)
            } else {
                12f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(220 + (index * 25), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pulseHeights.forEachIndexed { index, heightState ->
                val barHeight = heightState.value
                val color = if (isRecording) {
                    if (isPaused) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray.copy(alpha = 0.4f)
                }
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(4.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}
