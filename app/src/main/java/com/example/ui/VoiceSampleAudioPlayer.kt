package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VoiceClonerViewModel
import com.example.ui.theme.glassmorphic
import java.io.File

/**
 * A highly immersive, feature-rich, and gorgeous Audio Player Component for playing back 
 * recorded/uploaded voice samples with advanced speed, pitch, filter controls, and active waveform visualizers.
 */
@Composable
fun VoiceSampleAudioPlayer(
    viewModel: VoiceClonerViewModel,
    recordedFile: File,
    modifier: Modifier = Modifier
) {
    val isPlayingRecorded by viewModel.isPlayingRecorded.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playbackElapsedText by viewModel.playbackElapsedText.collectAsStateWithLifecycle()
    val playbackDurationText by viewModel.playbackDurationText.collectAsStateWithLifecycle()
    
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val playbackPitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val isMuted by viewModel.isPlayerMuted.collectAsStateWithLifecycle()
    val currentPreset by viewModel.acousticPreset.collectAsStateWithLifecycle()

    // Pulse animation for the active player theme
    val infiniteTransition = rememberInfiniteTransition(label = "audioWavePulse")
    val wavePulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val wavePulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )
    val wavePulseScale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(24.dp), elevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
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
                        contentDescription = "נגן דגימה",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "נגן דגימת קול מתקדם",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // File Details Label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(recordedFile.length() / 1024)} KB | WAV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Player Console
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Glow Play/Pause Circle
                IconButton(
                    onClick = {
                        if (isPlayingRecorded) {
                            viewModel.stopRecordedFile()
                        } else {
                            viewModel.playRecordedFile()
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("voice_player_play_btn")
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isPlayingRecorded) {
                                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
                                } else {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                }
                            )
                        )
                ) {
                    Icon(
                        imageVector = if (isPlayingRecorded) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingRecorded) "עצור דגימה" else "נגן דגימה",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPlayingRecorded) "מנגן דגימת קול..." else "דגימת קול מוכנה להשמעה",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayingRecorded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                        // Time indicators in monospace style
                        Text(
                            text = if (isPlayingRecorded) "$playbackElapsedText / $playbackDurationText" else "00:00 / --:--",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Adaptive interactive Waveform during playback
                    val mockAmplitudes = remember(recordedFile) {
                        List(50) { (0.15f + Math.random() * 0.75f).toFloat() }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val barWidth = 3.dp.toPx()
                            val spacing = 2.dp.toPx()
                            val totalBars = (width / (barWidth + spacing)).toInt()
                            
                            val step = if (mockAmplitudes.isNotEmpty()) mockAmplitudes.size.toFloat() / totalBars else 1f
                            
                            for (i in 0 until totalBars) {
                                val ampIndex = (i * step).toInt().coerceIn(0, maxOf(0, mockAmplitudes.size - 1))
                                var amp = if (mockAmplitudes.isNotEmpty()) mockAmplitudes[ampIndex] else 0.5f
                                
                                // Dynamic waving effect when active
                                if (isPlayingRecorded) {
                                    val pulseFactor = when (i % 3) {
                                        0 -> wavePulseScale1
                                        1 -> wavePulseScale2
                                        else -> wavePulseScale3
                                    }
                                    amp = (amp * (0.6f + 0.4f * pulseFactor)).coerceIn(0.1f, 1f)
                                }

                                val barHeight = (height * amp).coerceAtLeast(3.dp.toPx())
                                val startX = i * (barWidth + spacing)
                                
                                val isPlayed = (startX / width) <= playbackProgress
                                val barColor = if (isPlayed) {
                                    activeColorForIndex(i, totalBars)
                                } else {
                                    Color.Gray.copy(alpha = 0.25f)
                                }
                                
                                drawLine(
                                    color = barColor,
                                    start = androidx.compose.ui.geometry.Offset(startX + barWidth / 2, (height - barHeight) / 2),
                                    end = androidx.compose.ui.geometry.Offset(startX + barWidth / 2, (height + barHeight) / 2),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Precise Slider Controller
                    Slider(
                        value = if (isPlayingRecorded) playbackProgress else 0f,
                        onValueChange = { newValue ->
                            if (isPlayingRecorded) {
                                viewModel.seekPlayback(newValue)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("voice_player_progress_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-controllers layout: Speed & Pitch adjustments + Acoustic Filters
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title for Tuning controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "כוונון אינטונציה ומהירות שמע",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mute button with clear touch target
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.togglePlayerMute() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.Close else Icons.Default.Notifications,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color.Red else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMuted) "מושתק" else "פעיל",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMuted) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Playback Speed Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "מהירות דיבור (קצב):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("איטי", fontSize = 10.sp, color = Color.Gray)
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { viewModel.setPlaybackSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .testTag("voice_player_speed_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text("מהיר", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Playback Pitch Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "גובה צליל (טונאליות):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", playbackPitch),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("נמוך", fontSize = 10.sp, color = Color.Gray)
                            Slider(
                                value = playbackPitch,
                                onValueChange = { viewModel.setPlaybackPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .testTag("voice_player_pitch_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                            Text("גבוה", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Acoustic Filter Presets (Radio, Podcast, etc.)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "אפקט אקוסטיקה סביבתית (פילטר):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presetsList = listOf(
                                "None" to "רגיל 👤",
                                "Radio" to "רדיו 📻",
                                "Podcast" to "פודקאסט 🎤",
                                "Studio" to "אולפן 🎙️",
                                "Hall" to "אולם גדול 🏛️",
                                "Cathedral" to "קתדרלה ⛪"
                            )
                            presetsList.forEach { (presetKey, presetLabel) ->
                                val isSelected = currentPreset == presetKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        )
                                        .clickable { viewModel.setAcousticPreset(presetKey) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("voice_player_preset_$presetKey")
                                ) {
                                    Text(
                                        text = presetLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun activeColorForIndex(index: Int, total: Int): Color {
    val fraction = index.toFloat() / total
    return Color(
        red = (0.12f + fraction * 0.3f).coerceIn(0f, 1f),
        green = (0.45f + (1f - fraction) * 0.2f).coerceIn(0f, 1f),
        blue = (0.95f - fraction * 0.15f).coerceIn(0f, 1f)
    )
}
