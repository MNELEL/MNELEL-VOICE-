package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VoiceProfile
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure correct right-to-left layout for Hebrew language support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        VoiceClonerAppScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceClonerAppScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceClonerViewModel = viewModel()
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordedFile by viewModel.recordedFile.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizing.collectAsStateWithLifecycle()
    val synthesizeError by viewModel.synthesizeError.collectAsStateWithLifecycle()
    val isPlayingProfileId by viewModel.isPlayingProfileId.collectAsStateWithLifecycle()

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Form inputs
    var profileName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("נקבה") }
    val genders = listOf("נקבה", "זכר", "אחר")

    var expandedSynthProfileId by remember { mutableStateOf<Int?>(null) }
    var synthText by remember { mutableStateOf("") }

    val hasApiKey = remember {
        val key = BuildConfig.GEMINI_API_KEY
        key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Header with API Key Connection Status Indicator (Answers "Is API client updated")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "משבט קול AI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "עריכה, ניתוח ושיבוט קולות באמצעות Gemini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Connection Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasApiKey) Color(0x224CAF50) else Color(0x22FF9800)
                    )
                    .border(
                        width = 1.dp,
                        color = if (hasApiKey) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hasApiKey) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    )
                    Text(
                        text = if (hasApiKey) "מפתח מחובר" else "מפתח חסר .env",
                        fontSize = 11.sp,
                        color = if (hasApiKey) Color(0xFF81C784) else Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Core Recording Console
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recorder_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "הקלטת דגימה חדשה",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (micPermissionState.status.isGranted) {
                                // Recording animation indicator
                                SoundWaveVisualizer(isRecording = isRecording)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Pulse Microphone Button
                                val pulseScale by rememberInfiniteTransition().animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (isRecording) 1.2f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        if (isRecording) {
                                            viewModel.stopRecordVoice()
                                        } else {
                                            viewModel.startRecordVoice()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(72.dp)
                                        .animateContentSize()
                                        .testTag("mic_button")
                                        .clip(CircleShape)
                                        .background(
                                            if (isRecording) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = "הקלט",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isRecording) "מקליט... לחץ לעצירה" else "לחץ לשינוי והקלטת קול",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                // Analysis forms once recorded
                                if (recordedFile != null) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "הגדרות שיבוט",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = profileName,
                                        onValueChange = { profileName = it },
                                        label = { Text("שם הפרופיל (למשל 'יוסי')") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_name_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Gender Selection Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("מגדר:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        genders.forEach { gender ->
                                            FilterChip(
                                                selected = selectedGender == gender,
                                                onClick = { selectedGender = gender },
                                                label = { Text(gender) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            viewModel.cloneAndAnalyze(
                                                name = if (profileName.isEmpty()) "פרופיל ללא שם" else profileName,
                                                gender = selectedGender,
                                                description = "דגימת קול של $selectedGender לשכפול"
                                            )
                                            profileName = ""
                                        },
                                        enabled = !isAnalyzing,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("analyze_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isAnalyzing) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("מנתח תדרים ב-AI...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("נתח ושבט קול ב-Gemini AI", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Request permission
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "על מנת לשבט קול, יש לאפשר גישה למיקרופון המכשיר",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { micPermissionState.launchPermissionRequest() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("אשר גישה למיקרופון")
                                }
                            }

                            // Error logs
                            analysisError?.let { err ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Headline
                item {
                    Text(
                        text = "פרופילי קול משובטים (${profiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Empty state or profiles list
                if (profiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "אין פרופילי קול משובטים עדיין",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "הקלט דגימת קול מעלה ולחץ על שבט קול כדי לבצע ניתוח ושיבוט בבינה מלאכותית.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(profiles) { profile ->
                        VoiceProfileCard(
                            profile = profile,
                            isPlaying = isPlayingProfileId == profile.id,
                            isExpanded = expandedSynthProfileId == profile.id,
                            isSynthesizing = isSynthesizing,
                            synthText = synthText,
                            onSynthTextChange = { synthText = it },
                            onPlaySample = { viewModel.playProfileSample(profile) },
                            onStopSample = { viewModel.stopProfileSample() },
                            onToggleExpand = {
                                if (expandedSynthProfileId == profile.id) {
                                    expandedSynthProfileId = null
                                } else {
                                    expandedSynthProfileId = profile.id
                                    synthText = ""
                                }
                            },
                            onSynthesize = { text ->
                                viewModel.synthesizeText(text, profile)
                            },
                            onDelete = { viewModel.deleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceProfileCard(
    profile: VoiceProfile,
    isPlaying: Boolean,
    isExpanded: Boolean,
    isSynthesizing: Boolean,
    synthText: String,
    onSynthTextChange: (String) -> Unit,
    onPlaySample: () -> Unit,
    onStopSample: () -> Unit,
    onToggleExpand: () -> Unit,
    onSynthesize: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card_${profile.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Profile Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "מגדר: ${profile.gender} | דמיון קולי: ${profile.geminiVoiceName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_profile_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "מחק פרופיל",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Traits Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TraitChip(label = "גובה", value = profile.pitch, color = MaterialTheme.colorScheme.primary)
                TraitChip(label = "גוון", value = profile.tone, color = MaterialTheme.colorScheme.secondary)
                TraitChip(label = "קצב", value = profile.pace, color = MaterialTheme.colorScheme.tertiary)
                TraitChip(label = "אווירה", value = profile.vibe, color = Color(0xFFA5D6A7))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play Button
                Button(
                    onClick = {
                        if (isPlaying) onStopSample() else onPlaySample()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_sample_${profile.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "עצור דגימה" else "נגן דגימה מקורית",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Toggle Synth Panel Button
                Button(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("synthesize_toggle_${profile.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "דיבור דיגיטלי ב-AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Expanding Text-To-Speech Synthesis Core Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ייצור דיבור מטקסט בעזרת פרופיל הקול:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = synthText,
                        onValueChange = onSynthTextChange,
                        label = { Text("הקלד משפט קצר בעברית שהקול יקרא...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("synth_text_input_${profile.id}"),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSynthesize(synthText) },
                        enabled = !isSynthesizing && synthText.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("synth_submit_${profile.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSynthesizing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("מייצר שמע קולי ב-AI...")
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ייצר קול ונגן דיבור משובט")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TraitChip(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SoundWaveVisualizer(isRecording: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveHeights = List(12) { index ->
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = if (isRecording) (20f + (40f * ((index + 1) % 3))) else 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 250 + (index * 40), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Row(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(6.dp)
                    .height(height.value.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

val SoftWhite = Color(0xFFE5E7EB)
