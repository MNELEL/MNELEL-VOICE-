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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.AudioRecordingInterface
import com.example.ui.SynthesisLoadingCard
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var initialTab = 0
        var skipLanding = false
        val shortcutAction = intent?.getStringExtra("shortcut_action")
        if (shortcutAction == "record") {
            initialTab = 0
            skipLanding = true
        } else if (shortcutAction == "synthesis") {
            initialTab = 1
            skipLanding = true
        }
        
        setContent {
            MyApplicationTheme {
                // Ensure correct right-to-left layout for Hebrew language support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        VoiceClonerAppScreen(
                            modifier = Modifier.padding(innerPadding),
                            initialTab = initialTab,
                            skipLanding = skipLanding
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
    viewModel: VoiceClonerViewModel = viewModel(),
    initialTab: Int = 0,
    skipLanding: Boolean = false
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordedFile by viewModel.recordedFile.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val isGeminiAnalyzingAudio by viewModel.isGeminiAnalyzingAudio.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val audioAnalysisResult by viewModel.audioAnalysisResult.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizing.collectAsStateWithLifecycle()
    val synthesizeError by viewModel.synthesizeError.collectAsStateWithLifecycle()
    val isPlayingProfileId by viewModel.isPlayingProfileId.collectAsStateWithLifecycle()

    var showLandingPage by remember { mutableStateOf(!skipLanding) }
    var currentTab by remember { mutableStateOf(initialTab) }
    val templates by viewModel.allStyleTemplates.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("voice_app_prefs", android.content.Context.MODE_PRIVATE)
    var showRecordingTutorial by remember { mutableStateOf(prefs.getBoolean("show_recording_tutorial_v1", true)) }

    val isPlayingRecorded by viewModel.isPlayingRecorded.collectAsStateWithLifecycle()
    var isUploadMode by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAnalysisImportDialog by remember { mutableStateOf(false) }
    var showInfoModal by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val isRecordingPaused by viewModel.isRecordingPaused.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsStateWithLifecycle()
    val liveAmplitude by viewModel.liveAmplitude.collectAsStateWithLifecycle()
    val liveDecibels by viewModel.liveDecibels.collectAsStateWithLifecycle()
    val isNoiseMonitoring by viewModel.isNoiseMonitoring.collectAsStateWithLifecycle()
    val clarityScore by viewModel.clarityScore.collectAsStateWithLifecycle()
    val overallQualityScore by viewModel.overallQualityScore.collectAsStateWithLifecycle()
    val qualityFeedback by viewModel.qualityFeedback.collectAsStateWithLifecycle()

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

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    viewModel.importAllProfilesFromBackupBytes(bytes) { success, msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load backup file", e)
                android.widget.Toast.makeText(context, "שגיאה בקריאת קובץ: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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

    val hasApiKey by viewModel.isApiKeyAvailable.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }

    if (showLandingPage) {
        LandingPageScreen(
            viewModel = viewModel,
            onGetStarted = { showLandingPage = false },
            modifier = modifier
        )
        return
    }

    if (showRecordingTutorial) {
        AlertDialog(
            onDismissRequest = { 
                showRecordingTutorial = false
                prefs.edit().putBoolean("show_recording_tutorial_v1", false).apply()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("איך להקליט דגימה מנצחת? 🎙️", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "כדי שהשיבוט והניתוח הקולי יהיו מדוייקים ככל האפשר, אנא הקפידו על הכללים הבאים:", 
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val instructions = listOf(
                        "הקליטו בסביבה שקטה וללא רעשי רקע.",
                        "דברו בקול ברור וטבעי, ללא מבטא מאולץ.",
                        "הקפידו על הקלטה של 10 שניות לפחות.",
                        "קראו משפטים שלמים ושמרו על טון קול יציב."
                    )
                    
                    instructions.forEach { instruction ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Text(instruction, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showRecordingTutorial = false 
                        prefs.edit().putBoolean("show_recording_tutorial_v1", false).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("הבנתי, אפשר להתחיל ✨", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(com.example.ui.theme.PastelGradientBrush)
                .padding(16.dp)
        ) {
        // Rate Limit Global Notification
        val isWaitingForRateLimit by viewModel.isWaitingForRateLimit.collectAsStateWithLifecycle()
        val recoverySeconds by viewModel.rateLimitRecoverySeconds.collectAsStateWithLifecycle()
        
        if (isWaitingForRateLimit) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ מערכת במצב 'מנוחה' עקב מכסת API. אנא המתן ${recoverySeconds} שניות.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // App Header with API Key Connection Status Indicator (Answers "Is API client updated")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "משבט קול AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(
                        onClick = { showInfoModal = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "הנחיות שימוש ומדיניות",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { showSettingsPage = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "הגדרות",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = "עריכה, ניתוח ושיבוט קולות באמצעות Gemini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Clickable Connection Badge
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
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp).clickable { showApiKeyDialog = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = if (hasApiKey) "מפתח מחובר" else "הגדר מפתח API",
                        fontSize = 14.sp,
                        color = if (hasApiKey) Color(0xFF2E7D32) else Color(0xFFD84315),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Api Key configuration dialog
        if (showApiKeyDialog) {
            var tempKey by remember { mutableStateOf(customApiKey) }
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = {
                    Text(
                        text = "הגדרת מפתח Gemini API",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "לצורך ניתוח דיבור ושיבוט קולות, האפליקציה משתמשת בשירותי הבינה המלאכותית של Gemini. באפשרותך להגדיר מפתח אישי במידה ומפתח ברירת המחדל חסר או שאינו תקין (פתרון לשגיאה 103/403).",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        OutlinedTextField(
                            value = tempKey,
                            onValueChange = { tempKey = it },
                            label = { Text("מפתח Gemini API של גוגל", fontSize = 14.sp) },
                            placeholder = { Text("הכנס מפתח AI לכאן...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        val builtinKeyExists = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                        if (builtinKeyExists) {
                            Text(
                                text = "* קיים מפתח ברירת מחדל מובנה בשרת / קובץ .env",
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCustomApiKey(tempKey)
                            showApiKeyDialog = false
                            android.widget.Toast.makeText(context, "מפתח ה-API עודכן בהצלחה", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("שמור מפתח")
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (customApiKey.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.saveCustomApiKey("")
                                    tempKey = ""
                                    showApiKeyDialog = false
                                    android.widget.Toast.makeText(context, "מפתח אישי נמחק, חוזר לברירת מחדל", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("אפס לברירת מחדל", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = { showApiKeyDialog = false }) {
                            Text("ביטול")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isNarrow = configuration.screenWidthDp < 340

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple("בית", Icons.Default.Home, 0),
                        Triple("סינתזה", Icons.Default.PlayArrow, 1),
                        Triple("כלים", Icons.Default.Build, 8),
                        Triple("גלריה", Icons.Default.Person, 7)
                    )
                    items.forEach { item ->
                        val isSelected = currentTab == item.third || (item.third == 8 && (currentTab in 2..6 || currentTab == 9 || currentTab == 10))
                        NavigationBarItem(
                            icon = { Icon(item.second, contentDescription = item.first) },
                            label = { Text(item.first, maxLines = 1, fontSize = if (isNarrow) 10.sp else 12.sp) },
                            selected = isSelected,
                            alwaysShowLabel = !isNarrow,
                            onClick = { 
                                currentTab = item.third
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) { innerScaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerScaffoldPadding)
            ) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { fullWidth -> if (targetState > initialState) fullWidth else -fullWidth },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )).togetherWith(
                        androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { fullWidth -> if (targetState > initialState) -fullWidth else fullWidth },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        )
                    )
                },
                label = "Tab Transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // Section 1: Core Recording Console
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recorder_card")
                            .glassmorphic(shape = RoundedCornerShape(20.dp), elevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp)
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
                                        containerColor = if (!isUploadMode) BrandNavy else Color.Transparent,
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
                                        containerColor = if (isUploadMode) BrandNavy else Color.Transparent,
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
                                    AudioRecordingInterface(
                                        isRecording = isRecording,
                                        isPaused = isRecordingPaused,
                                        durationSec = recordingDurationSec, durationMs = recordingDurationMs,
                                        amplitude = liveAmplitude,
                                        onStart = { viewModel.startRecordVoice() },
                                        onPause = { viewModel.pauseRecordVoice() },
                                        onResume = { viewModel.resumeRecordVoice() },
                                        onStop = { viewModel.stopRecordVoice() },
                                        liveDecibels = liveDecibels,
                                        isNoiseMonitoring = isNoiseMonitoring,
                                        onToggleNoiseMonitoring = {
                                            if (isNoiseMonitoring) {
                                                viewModel.stopNoiseMonitoring()
                                            } else {
                                                viewModel.startNoiseMonitoring()
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isRecording) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier
                                                .fillMaxWidth(0.9f)
                                                .padding(horizontal = 8.dp)
                                                .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "📊 מדדי איכות ההקלטה בזמן אמת",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("צלילות השמע", fontSize = 14.sp, color = Color.Gray)
                                                        Text(
                                                            text = "$clarityScore%",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                clarityScore < 50 -> Color(0xFFEF4444)
                                                                clarityScore < 80 -> Color(0xFFF59E0B)
                                                                else -> Color(0xFF10B981)
                                                            }
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                             .height(30.dp)
                                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                    )

                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("ציון שיבוט כולל", fontSize = 14.sp, color = Color.Gray)
                                                        Text(
                                                            text = "$overallQualityScore%",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                overallQualityScore < 50 -> Color(0xFFEF4444)
                                                                overallQualityScore < 80 -> Color(0xFFF59E0B)
                                                                else -> Color(0xFF10B981)
                                                            }
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = qualityFeedback,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    color = when {
                                                        overallQualityScore < 50 -> Color(0xFFEF4444)
                                                        overallQualityScore < 80 -> Color(0xFFD97706)
                                                        else -> Color(0xFF059669)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
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
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
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
                                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp).clickable { filePickerLauncher.launch("audio/*") }
                                        .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                            fontSize = 14.sp,
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
                                        .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = if (isPlayingRecorded) "$playbackElapsedText / $playbackDurationText" else "00:00",
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Frequency Bar Visualizer for Playback
                                                if (isPlayingRecorded) {
                                                    // Simulated playback amplitude using a looping animation
                                                    val infiniteTransition = rememberInfiniteTransition()
                                                    val playAnimAmp by infiniteTransition.animateFloat(
                                                        initialValue = 0.3f,
                                                        targetValue = 0.9f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(400, easing = LinearEasing),
                                                            repeatMode = RepeatMode.Reverse
                                                        )
                                                    )
                                                    FrequencyVisualizer(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(30.dp)
                                                            .padding(horizontal = 8.dp),
                                                        isActive = isPlayingRecorded,
                                                        amplitude = playAnimAmp,
                                                        barCount = 20,
                                                        barColor = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }

                                                Slider(
                                                    value = if (isPlayingRecorded) playbackProgress else 0f,
                                                    onValueChange = { newValue ->
                                                        if (isPlayingRecorded) {
                                                            viewModel.seekPlayback(newValue)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(24.dp),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MaterialTheme.colorScheme.primary,
                                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gemini Audio Analysis Section
                                Text(
                                    text = "ניתוח אקוסטי מתקדם (Gemini AI)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (isGeminiAnalyzingAudio) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else {
                                    Button(
                                        onClick = { viewModel.analyzeAudioClip() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("נתח קובץ שמע באמצעות Gemini AI")
                                    }
                                }

                                audioAnalysisResult?.let { result ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("פרמטרים פונטיים:", fontWeight = FontWeight.Bold)
                                            Text(result.phoneticParameters, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("תדרי פיץ':", fontWeight = FontWeight.Bold)
                                            Text(result.pitchFrequencies, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("רמות רעש רקע:", fontWeight = FontWeight.Bold)
                                            Text(result.backgroundNoiseLevels, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("טביעת קול (Voice Print):", fontWeight = FontWeight.Bold)
                                            Text(result.voicePrint, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("עומק גרוני (Guttural Depth):", fontWeight = FontWeight.Bold)
                                            Text(result.gutturalDepth, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("חיתוך דיבור ודיקציה:", fontWeight = FontWeight.Bold)
                                            Text(result.dictionAndClipping, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("גוון קול וסגנון:", fontWeight = FontWeight.Bold)
                                            Text(result.voiceToneAndStyle, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            Text(result.overallSummary, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                OutlinedButton(
                                                    onClick = { viewModel.exportAnalysisToJson(context) },
                                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("גיבוי נתונים ל-JSON", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showAnalysisImportDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("שחזר מנתוני גיבוי JSON", fontSize = 12.sp)
                                }

                                analysisError?.let { err ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("מנתח תדרי קול [AI]...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("לחץ לניתוח קול ושכפול [AI]", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isAnalyzing) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                            fontSize = 14.sp,
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Html5AudioPlayer(viewModel = viewModel)
                }

                item {
                    com.example.ui.MultiSpeakerDiarizationView(
                        viewModel = viewModel,
                        profiles = profiles
                    )
                }

                // Section 2: Headline
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "פרופילי קול משובטים (${profiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandNavy.copy(alpha = 0.12f),
                                contentColor = BrandNavy
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ייבא חתימת קול 📥", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "גיבוי ונדידת פרופילים קבוצתית 🔄",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "שמור את כל פרופילי הקול והחתימות המשובטות בקובץ דחוס יחיד לצורך מעבר למכשיר אחר או שחזור מהיר.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.exportAllProfilesToBackupFile(context) { success, msg ->
                                            val finalMsg = if (success) "הייצוא הושלם בהצלחה! הקובץ נשמר בתיקיית ההורדות 📥" else msg
                                            android.widget.Toast.makeText(context, finalMsg, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ייצוא גיבוי קבוצתי", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        backupFilePickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ייבוא גיבוי קבוצתי", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Empty state or profiles list
                if (profiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                    fontSize = 14.sp,
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
                            onSynthesize = { text, pitchTuning, speedTuning, vibe, accent ->
                                viewModel.synthesizeText(text, profile, pitchTuning, speedTuning, vibe, accent)
                            },
                            onLocalSynthesize = { text, pitchTuning, speedTuning ->
                                viewModel.synthesizeTextLocal(text, profile, pitchTuning, speedTuning)
                            },
                            onExportClick = {
                                val json = viewModel.exportProfileToJson(profile)
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Voice Signature", json)
                                clipboardManager.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "חתימת הקול '${profile.name}' הועתקה ללוח הזיכרון (Clipboard)! 📋", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onDelete = { viewModel.deleteProfile(profile.id) },
                            recentGenerations = profileGenerations,
                            isPlayingResultId = isPlayingResultId,
                            onPlayResultSample = { viewModel.playResultSample(it) },
                            onStopResultSample = { viewModel.stopResultSample() },
                            onDeleteResult = { viewModel.deleteResult(it) },
                            templates = templates,
                            playbackProgress = playbackProgress
                        )
                    }
                }
            }
        }
        1 -> {
                SpeechSynthesisScreen(
                    viewModel = viewModel,
                    profiles = profiles,
                    templates = templates
                )
            }
            2 -> {
                StyleTemplatesScreen(
                    viewModel = viewModel,
                    templates = templates
                )
            }
            3 -> {
                MultiSpeakerDiarizationView(
                    viewModel = viewModel,
                    profiles = profiles
                )
            }
            4 -> {
                CalibrationTrainingScreen(
                    viewModel = viewModel,
                    profiles = profiles
                )
            }
            5 -> {
                PremiumCreditsScreen(
                    viewModel = viewModel
                )
            }
            6 -> {
                com.example.ui.DiagnosticComparisonScreen(
                    viewModel = viewModel
                )
            }
            7 -> {
                com.example.ui.VoiceProfileGallery(
                    profiles = profiles,
                    onSelectProfile = { /* Handle selection (e.g. switch to current tab 0) */ },
                    onDeleteProfile = { viewModel.deleteProfile(it.id) },
                    onRenameProfile = { profile, newName -> viewModel.renameProfile(profile.id, newName) },
                    onExportProfile = { profile -> viewModel.exportProfileToJson(profile, context) }
                )
            }
            8 -> {
                ToolsDashboardScreen(onNavigate = { currentTab = it })
            }
            9 -> {
                com.example.ui.OfflineCloningLabScreen(
                    viewModel = viewModel
                )
            }
            10 -> {
                com.example.ui.AudioBlobAnalyzerScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
}
}

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text(
                        text = "ייבוא חתימת קול משובט 📥",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandNavy
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "הדבק את קוד החתימה בפורמט JSON שיוצא בעבר:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("הדבק כאן את חתימת ה-JSON של הדובר...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                viewModel.importProfileFromJson(
                                    jsonStr = importJsonText,
                                    onSuccess = { msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                        showImportDialog = false
                                        importJsonText = ""
                                    },
                                    onError = { err ->
                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Text("ייבא למכשיר")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("ביטול")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showAnalysisImportDialog) {
            AlertDialog(
                onDismissRequest = { showAnalysisImportDialog = false },
                title = {
                    Text(
                        text = "שחזור נתוני ניתוח 📥",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "הדבק את קוד ה-JSON של האבחון הקולי:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("הדבק כאן את התוכן...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                viewModel.importAnalysisFromJson(importJsonText)
                                showAnalysisImportDialog = false
                                importJsonText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("שחזר אבחון")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAnalysisImportDialog = false }) {
                        Text("ביטול")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showSettingsPage) {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsPage = false }
            )
        }

        if (showInfoModal) {
            AlertDialog(
                onDismissRequest = { showInfoModal = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "הנחיות שימוש ומדיניות 🔐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 הנחיות שימוש להקלטה איכותית",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• הקלט בסביבה שקטה, ללא רעשי רקע של מאווררים, מזגנים או אנשים נוספים.\n" +
                                           "• דבר בטון דיבור טבעי ובקצב קבוע ורגוע.\n" +
                                           "• שמור על מרחק אחיד של כ-15-20 ס\"מ מהמיקרופון של המכשיר.",
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⏱️ דרישות משך הקלטה",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• מינימום מוחלט: 5 שניות. כל הקלטה קצרה מזאת תידחה על ידי מנוע העיבוד.\n" +
                                           "• מומלץ ביותר: 15-30 שניות. נפח קול זה מאפשר ניתוח תדרים, סטרקטורה פונטית ואינטונציה ברמה גבוהה.\n" +
                                           "• מקסימום: עד 3 דקות של דיבור רצוף.",
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🛡️ הגנת פרטיות והסכמה",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• כל נתוני הקול והניתוח מעובדים ומאובטחים בהתאם למדיניות אנונימית.\n" +
                                           "• חתימות הקול נשמרות בהתקן המקומי בלבד ואינן מועברות לגורמי צד שלישי כלשהם.\n" +
                                           "• חל איסור מוחלט לשכפל או לעשות שימוש בקולו של אדם אחר ללא הסכמתו המפורשת והחוקית.",
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("הבנתי ויאלה נתחיל!")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
        LiteRtDiagnosticOverlay(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomStart)
        )
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
    onSynthesize: (String, Float, Float, String, String) -> Unit,
    onLocalSynthesize: (String, Float, Float) -> Unit,
    onExportClick: () -> Unit,
    onDelete: () -> Unit,
    recentGenerations: List<com.example.data.VoiceGenerationResult>,
    isPlayingResultId: Int?,
    onPlayResultSample: (com.example.data.VoiceGenerationResult) -> Unit,
    onStopResultSample: () -> Unit,
    onDeleteResult: (com.example.data.VoiceGenerationResult) -> Unit,
    templates: List<com.example.data.VoiceStyleTemplate>,
    playbackProgress: Float = 0f
) {
    val context = LocalContext.current
    var userPitchTuning by remember { mutableStateOf(0f) }
    var userSpeedTuning by remember { mutableStateOf(0f) }
    var selectedVibeModifier by remember { mutableStateOf("מקורי") }
    var selectedAccent by remember { mutableStateOf("Standard") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card_${profile.id}")
            .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExportClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "ייצוא חתימת קול",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
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
                        fontSize = 14.sp
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
                        fontSize = 14.sp
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
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val speechRecognizerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val data = result.data
                            val matches = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                            if (!matches.isNullOrEmpty()) {
                                val newText = if (synthText.isNotEmpty()) synthText + " " + matches[0] else matches[0]
                                onSynthTextChange(newText)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = synthText,
                        onValueChange = onSynthTextChange,
                        label = { Text("הקלד משפט קצר בעברית שהקול יקרא...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("synth_text_input_${profile.id}"),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "דבר עכשיו...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.util.Log.e("SpeechToText", "Speech recognition not available", e)
                                }
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "הקלט טקסט בקול", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "הצעות מהירות לטקסט:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val suggestions = listOf(
                            "שלום, זהו קול הבדיקה שלי.",
                            "מערכת הבינה המלאכותית פועלת היטב.",
                            "אנא הקליטו בסביבה שקטה למניעת רעש."
                        )
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .clickable { onSynthTextChange(suggestion) }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic calibration card for 1:1 voice match tuning
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "לוח כיול קול דינמי (לחפיפת 1:1 בדיוק)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Pitch tuning slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "גובה צליל: ${if (userPitchTuning > 0) "+" else ""}${userPitchTuning.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (userPitchTuning == 0f) "ברירת מחדל" else if (userPitchTuning > 0) "דק / סופרן" else "עמוק / בס",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Slider(
                                value = userPitchTuning,
                                onValueChange = { userPitchTuning = it },
                                valueRange = -50f..50f,
                                modifier = Modifier.height(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Speed tuning slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "מהירות דיבור: ${if (userSpeedTuning > 0) "+" else ""}${userSpeedTuning.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (userSpeedTuning == 0f) "ברירת מחדל" else if (userSpeedTuning > 0) "מהיר" else "מדוד / איטי",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Slider(
                                value = userSpeedTuning,
                                onValueChange = { userSpeedTuning = it },
                                valueRange = -50f..50f,
                                modifier = Modifier.height(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Vibe Modifier chips selection
                            Text(
                                text = "גוון הבעה וסגנון רגשי קולי:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val vibes = listOf("מקורי", "סמכותי", "רגוע", "נמרץ", "דרמטי") + templates.map { it.name }
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(vibes.size) { index ->
                                    val vibe = vibes[index]
                                    val isSelected = selectedVibeModifier == vibe
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                            )
                                            .defaultMinSize(minWidth = 60.dp, minHeight = 44.dp)
                                            .clickable { selectedVibeModifier = vibe }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = vibe,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "מבטא והגייה (Accent):",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val accents = listOf(
                                "Standard" to "רגיל",
                                "Russian" to "רוסי",
                                "Moroccan" to "מרוקאי",
                                "Yemeni" to "תימני",
                                "American" to "אמריקאי",
                                "British" to "בריטי"
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(accents.size) { index ->
                                    val (accentKey, accentLabel) = accents[index]
                                    val isSelected = selectedAccent == accentKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                            )
                                            .defaultMinSize(minWidth = 60.dp, minHeight = 44.dp)
                                            .clickable { selectedAccent = accentKey }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = accentLabel,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSynthesize(synthText, userPitchTuning, userSpeedTuning, selectedVibeModifier, selectedAccent) },
                            enabled = !isSynthesizing && synthText.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .testTag("synth_submit_${profile.id}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSynthesizing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("מייצר קול...", fontSize = 14.sp)
                            } else {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("דיבור AI (שרת)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { onLocalSynthesize(synthText, userPitchTuning, userSpeedTuning) },
                            enabled = !isSynthesizing && synthText.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("השמע עם TTS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isSynthesizing) {
                        SynthesisLoadingCard()
                    }

                    // SWITCHER LIST OF RECENT CLONING HISTORY SAMPLES (Persistent Local State Storage)
                    if (recentGenerations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "הפקות שמע קודמות מהקול המשובט (${recentGenerations.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
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
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", result.createdAt)
                                        Text(
                                            text = "נוצר ב: $formattedDate",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        if (isPlayingResultId == result.id) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            FrequencyVisualizer(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(24.dp),
                                                isActive = true,
                                                amplitude = 0.3f + 0.6f * playbackProgress,
                                                barCount = 20,
                                                barColor = MaterialTheme.colorScheme.primary,
                                                bottomAligned = false
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick Regenerate Button
                                    IconButton(
                                        onClick = {
                                            onSynthTextChange(result.inputText)
                                            if (!isExpanded) onToggleExpand()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "הזן לשיבוט מחדש",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share Button
                                    IconButton(
                                        onClick = {
                                            shareAudioFile(
                                                context = context,
                                                sourceFile = java.io.File(result.audioPath)
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "שתף שמע",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

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
            .padding(vertical = 2.dp)
            .glassmorphic(shape = RoundedCornerShape(8.dp), elevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
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
fun VoiceDashboardSection(profile: VoiceProfile, viewModel: com.example.VoiceClonerViewModel? = null) {
    val activeViewModel: com.example.VoiceClonerViewModel = viewModel ?: androidx.lifecycle.viewmodel.compose.viewModel()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${profile.frequencyHz} Hz",
                    fontSize = 14.sp,
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
                        Text("בס (נמוך)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x1143A047)), contentAlignment = Alignment.Center) {
                        Text("בריטון", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x11E53935)), contentAlignment = Alignment.Center) {
                        Text("סופרן", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            com.example.ui.ClarityScoreVisualMeter(noiseLevel = profile.distortionLevel)
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricBarItem(label = "חיתוך דיבור ומאפייני הברה (Articulation/חיתוך)", value = profile.clarityScore, color = MaterialTheme.colorScheme.primary)
                MetricBarItem(label = "צורת הגייה ודיוק פונטי (Pronunciation/הגייה)", value = profile.pronunciationClarity, color = MaterialTheme.colorScheme.secondary)
                MetricBarItem(label = "אינטונציה ומנגינת דיבור (Intonation/התנגנות)", value = profile.intonationScore, color = MaterialTheme.colorScheme.tertiary)
                MetricBarItem(label = "סדירות נשימה והפסקות דיבור (Breathing/נשימה)", value = profile.breathPauseScore, color = Color(0xFF66BB6A))
            }

            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.AudioMetricsChart(profile = profile)

            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.VoiceAnalysisDashboardUI(profile = profile, viewModel = activeViewModel)
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
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Text(
                text = if (inverted) "$value% (נקי: $displayedPercentage%)" else "$value%",
                fontSize = 14.sp,
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
                fontSize = 14.sp,
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

fun shareAudioFile(context: android.content.Context, sourceFile: java.io.File) {
    if (!sourceFile.exists()) {
        android.widget.Toast.makeText(context, "קובץ השמע אינו זמין לשיתוף", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            sourceFile
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "audio/mpeg"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "שתף קובץ אודיו דרך..."))
    } catch (e: Exception) {
        android.util.Log.e("Share", "Failed to share file", e)
        android.widget.Toast.makeText(context, "שגיאה בשיתוף הקובץ", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun Html5AudioPlayer(
    viewModel: VoiceClonerViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val elapsed by viewModel.playbackElapsedText.collectAsStateWithLifecycle()
    val duration by viewModel.playbackDurationText.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val isMuted by viewModel.isPlayerMuted.collectAsStateWithLifecycle()
    val trackTitle by viewModel.playerTrackTitle.collectAsStateWithLifecycle()
    
    val isPlayingResultId by viewModel.isPlayingResultId.collectAsStateWithLifecycle()
    val isPlayingProfileId by viewModel.isPlayingProfileId.collectAsStateWithLifecycle()
    val isPlayingRecorded by viewModel.isPlayingRecorded.collectAsStateWithLifecycle()
    
    val isAnyPlaying = isPlayingResultId != null || isPlayingProfileId != null || isPlayingRecorded
    
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )
    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEA4335), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFBBC05), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF34A853), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "נגן אודיו מובנה 🌐 HTML5 Core",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5F6368)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${speed}x מהירות",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isAnyPlaying) Icons.Default.PlayArrow else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isAnyPlaying) Color(0xFF1E88E5) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isAnyPlaying) trackTitle else "אין שמע פעיל להפעלה",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3C4043),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (isAnyPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(2.dp).height((16 * waveScale1).dp).background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.width(2.dp).height((24 * waveScale2).dp).background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.width(2.dp).height((14 * waveScale3).dp).background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.width(2.dp).height((20 * waveScale1).dp).background(Color(0xFF4285F4)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = elapsed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5F6368)
                )

                Slider(
                    value = progress,
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4285F4),
                        activeTrackColor = Color(0xFF4285F4),
                        inactiveTrackColor = Color(0xFFDADCE0)
                    )
                )

                Text(
                    text = duration,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5F6368)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isAnyPlaying) {
                                viewModel.stopRecordedFile()
                                viewModel.stopProfileSample()
                                viewModel.stopResultSample()
                            } else {
                                viewModel.playRecordedFile()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFDADCE0), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isAnyPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = if (isAnyPlaying) "Stop" else "Play",
                            tint = if (isAnyPlaying) Color(0xFFEA4335) else Color(0xFF34A853),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayerMute() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFDADCE0), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.Close else Icons.Filled.Notifications,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color(0xFFEA4335) else Color(0xFF5F6368),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "ערוך והתאם אישית",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedSpeed = String.format(java.util.Locale.US, "%.1fx", speed)
                    Text(
                        text = "קצב ניגון: $formattedSpeed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5F6368),
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = speed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4285F4),
                            activeTrackColor = Color(0xFF4285F4),
                            inactiveTrackColor = Color(0xFFDADCE0)
                        )
                    )
                }

                // Pitch Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedPitch = String.format(java.util.Locale.US, "%.1fx", pitch)
                    Text(
                        text = "גובה צליל: $formattedPitch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5F6368),
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.setPlaybackPitch(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF34A853),
                            activeTrackColor = Color(0xFF34A853),
                            inactiveTrackColor = Color(0xFFDADCE0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFDADCE0))
            Spacer(modifier = Modifier.height(6.dp))

            val currentPreset by viewModel.acousticPreset.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ייצוא או פילטרים ועריכת אקוסטיקה (אפקט Reverbs):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5F6368)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "None" to "רגיל 👤",
                        "Studio" to "אולפן 🎙️",
                        "Room" to "חדר 🏠",
                        "Hall" to "אולם 🏛️",
                        "Cathedral" to "הד ⛪"
                    )
                    
                    presets.forEach { (presetKey, displayName) ->
                        val isSelected = currentPreset == presetKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF4285F4) else Color.White)
                                .border(1.dp, if (isSelected) Color(0xFF4285F4) else Color(0xFFDADCE0), RoundedCornerShape(8.dp))
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .clickable {
                                    viewModel.setAcousticPreset(presetKey)
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF5F6368),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: VoiceClonerViewModel,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    
    val context = LocalContext.current
    val datePickerState = androidx.compose.material3.rememberDatePickerState()
    
    if (showDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("אישור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("ביטול")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("הגדרות חשבון", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "מועד מחיקה מתוזמן (אופציונלי):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    val dateStr = if (selectedDateMillis != null) {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(selectedDateMillis!!))
                    } else "בחר תאריך יעד..."
                    Text(text = dateStr, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "גיבוי ענן ושחזור (Drive)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ייצא את הנתונים שלך לענן כדי לגבות אותם או לשחזר אותם במכשיר אחר.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedButton(
                    onClick = { 
                        android.widget.Toast.makeText(context, "פותח חיבור לכונן ענן...", android.widget.Toast.LENGTH_SHORT).show()
                        // Simulate Drive Backup connection intent or start activity
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("גיבוי לענן", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "פתרון בעיות (Troubleshooting)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = {
                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.voicecloner.com/support"))
                        try {
                            context.startActivity(browserIntent)
                        } catch(e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("עזרה ופתרון בעיות", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "סגירת החשבון",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "מחיקת החשבון תסיר לצמיתות את כל פרופילי הקול וההקלטות ממסד הנתונים.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        android.widget.Toast.makeText(context, "כל הנתונים והחשבון נמחקו לצמיתות.", android.widget.Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("מחק חשבון ונתונים", fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("סגור", fontSize = 16.sp)
            }
        }
    )
}

@Composable
fun LiteRtDiagnosticOverlay(
    viewModel: VoiceClonerViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val status by viewModel.liteRtStatus.collectAsStateWithLifecycle()
    val speed by viewModel.liteRtProcessingSpeed.collectAsStateWithLifecycle()
    val memory by viewModel.liteRtMemoryUsage.collectAsStateWithLifecycle()
    val cpu by viewModel.liteRtCpuUsage.collectAsStateWithLifecycle()
    val delegate by viewModel.liteRtHardwareDelegate.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        if (!isExpanded) {
            FloatingActionButton(
                onClick = { isExpanded = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("litert_diagnostic_fab")
                    .size(56.dp),
                shape = CircleShape
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "מצב LiteRT",
                        modifier = Modifier.size(22.dp)
                    )
                    Text("אבחון", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .testTag("litert_diagnostic_card")
                    .clickable(enabled = false) {},
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "אבחון וניטור מנוע LiteRT-LM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "סגור",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("סטטוס מנוע בזמן אמת:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        
                        val isProcessing = status == "Active"
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isProcessing) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isProcessing) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (isProcessing) "בפעילות סינתזה" else "בהמתנה (Idle)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProcessing) Color(0xFF388E3C) else Color.Gray
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("זמן שיהוי (Latency)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                val latencyVal = if (speed > 0) String.format("%.0f ms", 1000f / speed) else "0 ms"
                                Text(latencyVal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("מהירות: ${String.format("%.1f", speed)} t/s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ניצול זיכרון RAM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$memory MB", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                val memProgress = (memory / 256f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { memProgress },
                                    color = if (memProgress > 0.7f) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("עומס מעבד (CPU)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$cpu%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                val cpuProgress = (cpu / 100f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { cpuProgress },
                                    color = if (cpuProgress > 0.5f) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("מאיץ חומרה", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(delegate, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (delegate.contains("GPU")) "מואץ חומרה ⚡" else "עיבוד תוכנתי 🐌", fontSize = 9.sp, color = if (delegate.contains("GPU")) Color(0xFF388E3C) else Color(0xFFD32F2F))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                delegate.contains("CPU") -> Color(0xFFFFF9C4).copy(alpha = 0.5f)
                                memory > 180 -> Color(0xFFFFCDD2).copy(alpha = 0.4f)
                                cpu > 50 -> Color(0xFFFFE0B2).copy(alpha = 0.4f)
                                else -> Color(0xFFC8E6C9).copy(alpha = 0.3f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                delegate.contains("CPU") -> Color(0xFFFBC02D)
                                memory > 180 -> Color(0xFFE53935)
                                cpu > 50 -> Color(0xFFF57C00)
                                else -> Color(0xFF81C784)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    delegate.contains("CPU") -> "⚠️"
                                    memory > 180 -> "🚨"
                                    cpu > 50 -> "⚡"
                                    else -> "✅"
                                },
                                fontSize = 18.sp
                            )
                            Column {
                                Text(
                                    text = when {
                                        delegate.contains("CPU") -> "זוהה צוואר בקבוק: עיבוד במעבד (CPU)"
                                        memory > 180 -> "זוהה צוואר בקבוק: צריכת זיכרון חריגה"
                                        cpu > 50 -> "זוהה צוואר בקבוק: עומס מעבד חריג"
                                        else -> "אופטימיזציה מלאה"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = when {
                                        delegate.contains("CPU") -> "החומרה אינה מואצת באמצעות מנגנון NNAPI של ה-GPU. מהירות הסינתזה עלולה לרדת. פתרון: ודא שהאצת חומרה מופעלת בהגדרות הכלליות."
                                        memory > 180 -> "מודל ה-LiteRT-LM מנצל כמות זיכרון גבוהה ($memory MB). פתרון: מומלץ לסגור אפליקציות הפועלות ברקע כדי למנוע קריסה מונעת מה-OS."
                                        cpu > 50 -> "עומס העיבוד המקומי גבוה מאוד ($cpu%). פתרון: הימנע מהרצת משימות סינתזה כבדות בו-זמנית ותן למכשיר להתקרר."
                                        else -> "ביצועי מנוע ה-LiteRT-LM אופטימליים! חתימות הקול מעובדות במהירות מקסימלית על גבי המאיץ הגרפי ללא צווארי בקבוק."
                                    },
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startMetricsFluctuation() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("הפעל בדיקת עומס (Stress Test)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("סגור", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


