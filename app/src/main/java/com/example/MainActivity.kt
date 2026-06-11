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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.util.Log

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

    val context = LocalContext.current
    val isPlayingRecorded by viewModel.isPlayingRecorded.collectAsStateWithLifecycle()
    var isUploadMode by remember { mutableStateOf(false) }

    val isRecordingPaused by viewModel.isRecordingPaused.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val liveAmplitude by viewModel.liveAmplitude.collectAsStateWithLifecycle()

    val recentGenerations by viewModel.recentGenerations.collectAsStateWithLifecycle()
    val isPlayingResultId by viewModel.isPlayingResultId.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playbackElapsedText by viewModel.playbackElapsedText.collectAsStateWithLifecycle()
    val playbackDurationText by viewModel.playbackDurationText.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.let { stream ->
                    var fileName = "uploaded_audio.aac"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    viewModel.uploadAudioStream(stream, fileName)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to choose audio file", e)
            }
        }
    }

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
                        color = if (hasApiKey) Color(0xFF2E7D32) else Color(0xFFD84315),
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
                            // Tab Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { isUploadMode = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isUploadMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (!isUploadMode) Color.White else MaterialTheme.colorScheme.onBackground
                                    ),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("הקלטת דגימה", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Button(
                                    onClick = { isUploadMode = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUploadMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isUploadMode) Color.White else MaterialTheme.colorScheme.onBackground
                                    ),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("העלאת דגימה", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!isUploadMode) {
                                // Recording Mode Content
                                Text(
                                    text = "הקלטת דגימה בקולך",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (micPermissionState.status.isGranted) {
                                    SoundWaveVisualizer(
                                        isRecording = isRecording,
                                        isPaused = isRecordingPaused,
                                        amplitude = liveAmplitude
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isRecording) {
                                        // Display formatted recording duration and pause/resume triggers
                                        val minutes = recordingDurationSec / 60
                                        val seconds = recordingDurationSec % 60
                                        val durationText = String.format("%02d:%02d", minutes, seconds)

                                        Text(
                                            text = "זמן הקלטה: $durationText",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            ),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Pause/Resume recording action trigger
                                            IconButton(
                                                onClick = {
                                                    if (isRecordingPaused) {
                                                        viewModel.resumeRecordVoice()
                                                    } else {
                                                        viewModel.pauseRecordVoice()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape)
                                            ) {
                                                if (isRecordingPaused) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "המשך הקלטה",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    // Beautiful custom vector pause bar representation
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(modifier = Modifier.width(4.dp).height(16.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                                        Box(modifier = Modifier.width(4.dp).height(16.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                                    }
                                                }
                                            }

                                            // Stop & save recording trigger
                                            IconButton(
                                                onClick = {
                                                    viewModel.stopRecordVoice()
                                                },
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "סיום ושמירה",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isRecordingPaused) "ההקלטה מושהית" else "הקלטה קולית פעילה...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                viewModel.startRecordVoice()
                                            },
                                            modifier = Modifier
                                                .size(72.dp)
                                                .testTag("mic_button")
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "הקלט",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "לחץ להתחלת הקלטת קול",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                } else {
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
                            } else {
                                // Upload Mode Content
                                Text(
                                    text = "העלאת קובץ קול",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { filePickerLauncher.launch("audio/*") },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "לחץ לבחירת קובץ שמע מהמכשיר",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "תומך בפורמטים MP3, AAC, WAV ועוד",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            // Shared Section: If sample loaded (recorded or uploaded)
                            if (recordedFile != null) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
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
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "דגימת קול מוכנה לשיבוט",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Clear Button
                                            IconButton(
                                                onClick = {
                                                    viewModel.clearRecordedFile()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "מחק דגימה",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Listen to chosen sample premium playback component
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (isPlayingRecorded) {
                                                        viewModel.stopRecordedFile()
                                                    } else {
                                                        viewModel.playRecordedFile()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isPlayingRecorded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingRecorded) Icons.Default.Close else Icons.Default.PlayArrow,
                                                    contentDescription = "שמע דגימה",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isPlayingRecorded) "מנגן דגימת קול..." else "שמע דגימת שנטענה",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = if (isPlayingRecorded) "$playbackElapsedText / $playbackDurationText" else "00:00",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = { if (isPlayingRecorded) playbackProgress else 0f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "הגדרות פרופיל קול משובט",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    label = { Text("שם הפרופיל (למשל 'דוד')") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        Text("מנתח תדרי קול [AI]...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("לחץ לניתוח קול ושכפול [AI]", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isAnalyzing) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "ניתוח ושיבוט קול בעיצומו [AI]",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Dynamic stepping logs
                                        val stepText = remember {
                                            listOf(
                                                "מפענח דגימת הקול שהוזנה...",
                                                "מסננן רעשי רקע ותדרים משניים...",
                                                "מנתח גוון קול, גובה ותווים פיזיקליים...",
                                                "משלב חתימת קול עם מודל השפה של Gemini...",
                                                "מייצר פרופיל קול דיגיטלי מותאם אישית..."
                                            )
                                        }
                                        var currentStepIdx by remember { mutableStateOf(0) }
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                kotlinx.coroutines.delay(2000)
                                                currentStepIdx = (currentStepIdx + 1) % stepText.size
                                            }
                                        }
                                        
                                        Text(
                                            text = stepText[currentStepIdx],
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
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
                        val profileGenerations = recentGenerations.filter { it.profileId == profile.id }
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
                            onDelete = { viewModel.deleteProfile(profile.id) },
                            recentGenerations = profileGenerations,
                            isPlayingResultId = isPlayingResultId,
                            onPlayResultSample = { viewModel.playResultSample(it) },
                            onStopResultSample = { viewModel.stopResultSample() },
                            onDeleteResult = { viewModel.deleteResult(it) }
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
    onDelete: () -> Unit,
    recentGenerations: List<com.example.data.VoiceGenerationResult>,
    isPlayingResultId: Int?,
    onPlayResultSample: (com.example.data.VoiceGenerationResult) -> Unit,
    onStopResultSample: () -> Unit,
    onDeleteResult: (com.example.data.VoiceGenerationResult) -> Unit
) {
    val context = LocalContext.current
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

            // Expandable technical diagnostic voice analysis metrics dashboard
            VoiceDashboardSection(profile = profile)

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

                    // SWITCHER LIST OF RECENT CLONING HISTORY SAMPLES (Persistent Local State Storage)
                    if (recentGenerations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "הפקות שמע קודמות מהקול המשובט (${recentGenerations.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        recentGenerations.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingResultId == result.id) {
                                                onStopResultSample()
                                            } else {
                                                onPlayResultSample(result)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingResultId == result.id) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.inputText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", result.createdAt)
                                        Text(
                                            text = "נוצר ב: $formattedDate",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Download Button (API 29+ Safe Downloads)
                                    IconButton(
                                        onClick = {
                                            downloadFileToDevice(
                                                context = context,
                                                sourceFile = java.io.File(result.audioPath),
                                                displayName = "משבט-קול-${result.profileName}-${result.id}"
                                            ) { success, message ->
                                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "הורד שמע",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { onDeleteResult(result) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "מחק הפקה",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
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
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SoundWaveVisualizer(
    isRecording: Boolean,
    isPaused: Boolean = false,
    amplitude: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveHeights = List(12) { index ->
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = if (isRecording && !isPaused) {
                // Combine real-time amplitude with index-based frequency offset
                val ampFactor = if (amplitude > 0.05f) amplitude * 80f else 15f
                val indexBonus = (index % 4) * 8f
                (12f + ampFactor + indexBonus).coerceIn(10f, 75f)
            } else if (isRecording && isPaused) {
                12f // static small flat line when paused
            } else {
                10f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 200 + (index * 30), easing = FastOutSlowInEasing),
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
                            colors = if (isPaused) {
                                listOf(Color.Gray.copy(alpha = 0.5f), Color.LightGray.copy(alpha = 0.3f))
                            } else {
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            }
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

val SoftWhite = Color(0xFFE5E7EB)

@Composable
fun VoiceDashboardSection(profile: VoiceProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "לוח בקרה וניתוח תדרי קול",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "תדר קול ממוצע:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${profile.frequencyHz} Hz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x111E88E5)), contentAlignment = Alignment.Center) {
                        Text("בס (נמוך)", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x1143A047)), contentAlignment = Alignment.Center) {
                        Text("בריטון", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x11E53935)), contentAlignment = Alignment.Center) {
                        Text("סופרן", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                
                val fraction = ((profile.frequencyHz - 60f) / 220f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricBarItem(label = "חיתוך דיבור ומאפייני הברה (Articulation/חיתוך)", value = profile.clarityScore, color = MaterialTheme.colorScheme.primary)
                MetricBarItem(label = "צורת הגייה ודיוק פונטי (Pronunciation/הגייה)", value = profile.pronunciationClarity, color = MaterialTheme.colorScheme.secondary)
                MetricBarItem(label = "אינטונציה ומנגינת דיבור (Intonation/התנגנות)", value = profile.intonationScore, color = MaterialTheme.colorScheme.tertiary)
                MetricBarItem(label = "סדירות נשימה והפסקות דיבור (Breathing/נשימה)", value = profile.breathPauseScore, color = Color(0xFF66BB6A))
                MetricBarItem(label = "מדד רעש רקע ועיוותי שפה (Noise/عيوותים)", value = profile.distortionLevel, inverted = true, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun MetricBarItem(label: String, value: Int, inverted: Boolean = false, color: Color) {
    val displayedPercentage = if (inverted) 100 - value else value
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Text(
                text = if (inverted) "$value% (נקי: $displayedPercentage%)" else "$value%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (inverted && value > 30) MaterialTheme.colorScheme.error else color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LinearProgressIndicator(
                progress = { value.toFloat() / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.12f)
            )
            val qualityLabel = when {
                inverted && value < 15 -> "מעולה"
                inverted && value < 30 -> "טוב"
                inverted -> "רעשי רקע"
                value > 85 -> "מצוין"
                value > 70 -> "טוב מאוד"
                value > 50 -> "בינוני"
                else -> "נמוך"
            }
            Text(
                text = qualityLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun downloadFileToDevice(
    context: android.content.Context,
    sourceFile: java.io.File,
    displayName: String,
    onResult: (Boolean, String) -> Unit
) {
    if (!sourceFile.exists()) {
        onResult(false, "קובץ השמע המשובט אינו זמין להורדה")
        return
    }
    
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.mp3")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = java.io.File(downloadsDir, "$displayName.mp3")
            android.net.Uri.fromFile(targetFile)
        }
        
        if (uri == null) {
            onResult(false, "כשל ביצירת קובץ ההורדה במערכת")
            return
        }
        
        resolver.openOutputStream(uri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        
        onResult(true, "הורדה הושלמה! הקובץ נשמר בתיקיית ההורדות של המכשיר.")
    } catch (e: Exception) {
        android.util.Log.e("Download", "Failed to download", e)
        onResult(false, "כשל הורדה: ${e.message}")
    }
}
