package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix: Use Singleton AppDatabase to prevent runtime SQLite crashes and multiple database connection lockups
        db = AppDatabase.getDatabase(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: VoiceClonerViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return VoiceClonerViewModel(db, applicationContext) as T
                    }
                })
                
                // Force RTL Layout so Hebrew strings align perfectly from right-to-left regardless of system location settings
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        VoiceClonerApp(viewModel, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceClonerApp(viewModel: VoiceClonerViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecording()
        } else {
            Toast.makeText(context, "אפליקציית שיבוט הקול זקוקה לאישור המיקרופון שלך כדי להקליט ולנתח שמע.", Toast.LENGTH_LONG).show()
        }
    }

    if (showSettings) {
        var tempKey by remember { mutableStateOf(uiState.apiKey) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = {
                Text(
                    text = "הגדרות מפתח API (רשת Gemini)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "להפעלת שיבוט וניתוח קול מלא באמצעות בינה מלאכותית (כולל הקראה קולית ייחודית ב-TTS), אנא הזן מפתח Gemini API בחינם מ-Google AI Studio.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("הדבק מפתח כאן (AI Studio)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            try {
                                val uri = android.net.Uri.parse("https://aistudio.google.com/")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "לא נמצא דפדפן זמין לפתיחת הקישור.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Open Link")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("לקבלת מפתח API בחינם מ-Google AI Studio", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "• המפתח נשמר במכשיר שלך באופן מאובטח (SharedPreferences) ואינו מועבר לשום שרת חיצוני.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiKey(tempKey.trim())
                        showSettings = false
                        Toast.makeText(context, "מפתח ה-API עודכן בהצלחה!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("שמור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("ביטול")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { showSettings = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // Stepper Status Indicators
        VoiceClonerStepper(currentStep = uiState.currentStep)

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area with elegant animations
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.currentStep) {
                ClonerStep.RECORDING -> {
                    RecordingStepView(
                        isRecording = uiState.isRecording,
                        recordingDuration = uiState.recordingDuration,
                        onToggleRecording = {
                            if (uiState.isRecording) {
                                viewModel.toggleRecording()
                            } else {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.toggleRecording()
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onNext = { viewModel.setStep(ClonerStep.ANALYSIS) }
                    )
                }
                ClonerStep.ANALYSIS -> {
                    AnalysisStepView(
                        uiState = uiState,
                        onSelectedAccent = { viewModel.setSelectedAccent(it) },
                        onTogglePhoneticAccuracy = { viewModel.togglePhoneticAccuracy(it) },
                        onUpdatePitch = { viewModel.updatePitchLevel(it) },
                        onUpdateSpeed = { viewModel.updateSpeedLevel(it) },
                        onUpdateEmotionalDepth = { viewModel.updateEmotionalDepth(it) },
                        onSaveProfile = { name ->
                            viewModel.saveProfile(name)
                            viewModel.setStep(ClonerStep.SAVING)
                        }
                    )
                }
                ClonerStep.SAVING -> {
                    SavingStepView(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceClonerStepper(currentStep: ClonerStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf(
            ClonerStep.RECORDING to stringResource(R.string.step_1_recording),
            ClonerStep.ANALYSIS to stringResource(R.string.step_2_analysis_settings),
            ClonerStep.SAVING to stringResource(R.string.step_3_saved_successfully)
        )

        steps.forEachIndexed { index, (step, label) ->
            val isActive = currentStep == step
            val isCompleted = currentStep.ordinal > step.ordinal
            
            val containerColor = when {
                isActive -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val textColor = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                isCompleted -> MaterialTheme.colorScheme.onSecondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(containerColor.copy(alpha = if (isActive || isCompleted) 1.0f else 0.4f))
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            if (index < steps.size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(horizontal = 4.dp),
                    color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecordingStepView(
    isRecording: Boolean,
    recordingDuration: Int,
    onToggleRecording: () -> Unit,
    onNext: () -> Unit
) {
    val durationText = String.format(java.util.Locale.US, "%02d:%02d", recordingDuration / 60, recordingDuration % 60)
    
    // Wave animation parameters
    val infiniteTransition = rememberInfiniteTransition(label = "Waves")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.recording_hint),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Target reading practice script for Hebrew
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = stringResource(R.string.sample_read_paragraph),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Beautiful audio wave visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    val lineColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        if (width.isNaN() || width.isInfinite() || height.isNaN() || height.isInfinite() || width <= 0f || height <= 0f || width > 5000f || height > 5000f) return@Canvas
                        val midY = height / 2f
                        
                        // Let's draw 3 overlaid sine waves for a modern professional acoustic appearance
                        val wavesCount = 3
                        val safeWaveOffset = if (waveOffset.isNaN() || waveOffset.isInfinite()) 0f else waveOffset
                        for (w in 0 until wavesCount) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, midY)
                            
                            val baseSin = sin(safeWaveOffset * 0.05f + w * 2f)
                            val safeBaseSin = if (baseSin.isNaN() || baseSin.isInfinite()) 0f else baseSin.coerceIn(-1f, 1f)
                            val amplitude = (30f + w * 15f) * safeBaseSin
                            val frequency = 0.015f + w * 0.005f
                            
                            for (x in 0..width.toInt() step 5) {
                                val s = sin(x * frequency + safeWaveOffset * 0.05f)
                                val safeS = if (s.isNaN() || s.isInfinite()) 0f else s
                                val y = midY + amplitude * safeS
                                path.lineTo(x.toFloat(), y)
                            }
                            drawPath(
                                path = path,
                                color = lineColor.copy(alpha = 1.0f - (w * 0.25f)),
                                style = Stroke(width = 8f, cap = StrokeCap.Round)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "ממתין להקלטה...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${stringResource(R.string.recording_duration)} $durationText",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(
                    onClick = onToggleRecording,
                    modifier = Modifier.size(64.dp),
                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = "Record Control",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.finish_recording_and_analyze),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnalysisStepView(
    uiState: VoiceClonerUiState,
    onSelectedAccent: (String) -> Unit,
    onTogglePhoneticAccuracy: (Boolean) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateEmotionalDepth: (Float) -> Unit,
    onSaveProfile: (String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var expandedMenu by remember { mutableStateOf(false) }

    val accentsList = listOf(
        "מבטא ישראלי צברי",
        "מבטא אנגלו-סכסי",
        "מבטא רוסי סמכותי",
        "מבטא ערבי עדין",
        "מבטא מזרחי מסורתי"
    )

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.cloning_parameters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "כוונן את מאפייני אלגוריתם שיבוט הקול:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Name field
            item {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    placeholder = { Text("לדוגמה: הקול של רוני") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Accent Dropdown Box
            item {
                Text(
                    text = stringResource(R.string.select_accent),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = { expandedMenu = !expandedMenu },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = uiState.selectedAccent, style = MaterialTheme.typography.bodyLarge)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        accentsList.forEach { accent ->
                            DropdownMenuItem(
                                text = { Text(accent) },
                                onClick = {
                                    onSelectedAccent(accent)
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Sliders block
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Average Pitch
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.pitch_level), style = MaterialTheme.typography.labelMedium)
                        Text(text = "${(uiState.pitchLevel * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = uiState.pitchLevel,
                        onValueChange = onUpdatePitch,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Average Speech speed
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.speed_level), style = MaterialTheme.typography.labelMedium)
                        Text(text = "${(uiState.speedLevel * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = uiState.speedLevel,
                        onValueChange = onUpdateSpeed,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Average Emotional depth
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.emotional_depth), style = MaterialTheme.typography.labelMedium)
                        Text(text = "${(uiState.emotionalDepth * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = uiState.emotionalDepth,
                        onValueChange = onUpdateEmotionalDepth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Checkbox options
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePhoneticAccuracy(!uiState.phoneticAccuracy) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = uiState.phoneticAccuracy,
                        onCheckedChange = { onTogglePhoneticAccuracy(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.phonetic_accuracy_mode),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "משפר את התאמת הברות וחלקי מילים ייחודיים בעברית",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Save Action
            item {
                if (uiState.isAnalyzing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "מנתח דגימת שמע בבינה מלאכותית...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val inputName = profileName.trim().ifEmpty { "פרופיל קולי" }
                            onSaveProfile(inputName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.save_profile), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SavingStepView(
    viewModel: VoiceClonerViewModel,
    uiState: VoiceClonerUiState
) {
    val profiles by viewModel.savedProfiles.collectAsState(initial = emptyList())
    var testingText by remember { mutableStateOf("שלום! זהו ניסיון הקראה מיידי של הקול המשובט החדש שלי.") }

    val infiniteTransition = rememberInfiniteTransition(label = "WavesSpeak")
    val speechOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpeechWave"
    )

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.step_3_saved_successfully),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "כעת תוכל לבחור פרופיל ולהשמיע קטעי דיבור מותאמים אישית.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TTS Test Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.test_text),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = testingText,
                        onValueChange = { testingText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.speakingText != null) {
                        // Glowing animated wavelength reading indicator
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "משבוט קולי (${uiState.speakingProfileName}) מקריא כעת...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Speaking acoustic waveform
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                            ) {
                                val width = size.width
                                val height = size.height
                                if (width.isNaN() || width.isInfinite() || height.isNaN() || height.isInfinite() || width <= 0f || height <= 0f || width > 5000f || height > 5000f) return@Canvas
                                val midY = height / 2f
                                val bars = 40
                                val spacing = width / bars
                                
                                val safeSpeechOffset = if (speechOffset.isNaN() || speechOffset.isInfinite()) 0f else speechOffset
                                for (i in 0 until bars) {
                                    val x = i * spacing
                                    val s = sin(i * 0.3f + safeSpeechOffset * 0.1f)
                                    val factor = if (s.isNaN() || s.isInfinite()) 0f else s
                                    val barHeight = (height * 0.8f) * kotlin.math.abs(factor)
                                    val safeBarHeight = if (barHeight.isNaN() || barHeight.isInfinite()) 0f else barHeight
                                    drawLine(
                                        color = lineColor(i),
                                        start = androidx.compose.ui.geometry.Offset(x, midY - safeBarHeight / 2f),
                                        end = androidx.compose.ui.geometry.Offset(x, midY + safeBarHeight / 2f),
                                        strokeWidth = 8f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.stopSpeakingSimulation() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = "עצור השמעה")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main List Header with Action
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.existing_profiles),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (profiles.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clickable { viewModel.clearAllProfiles() }
                            .padding(4.dp)
                    )
                }
            }

            // Profiles list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "אין פרופילים קוליים שמורים כעת. שמור פרופיל כדי להתחיל.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(profiles, key = { it.id }) { profile ->
                        VoiceProfileRow(
                            profile = profile,
                            isPlaying = uiState.isPlaying && uiState.playingProfileId == profile.id,
                            onPlayRecord = {
                                if (uiState.isPlaying && uiState.playingProfileId == profile.id) {
                                    viewModel.stopProfileRecordingPlayback()
                                } else {
                                    viewModel.playProfileRecording(profile)
                                }
                            },
                            onSpeak = {
                                viewModel.startSpeakingSimulation(testingText, profile.name)
                            },
                            onDelete = {
                                viewModel.deleteProfile(profile)
                            }
                        )
                    }
                }
            }

            // Navigation back down
            Button(
                onClick = { viewModel.setStep(ClonerStep.RECORDING) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.start_over), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Draw nice premium wave bars using a predefined safe Color palette
private fun lineColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
        Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFD54F),
        Color(0xFFFFB74D), Color(0xFFFF8A65)
    )
    return colors[kotlin.math.abs(index) % colors.size]
}

@Composable
fun VoiceProfileRow(
    profile: VoiceProfile,
    isPlaying: Boolean,
    onPlayRecord: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Voice Profile",
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (!expanded) {
                            Text(
                                text = profile.analysisData,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Interaction icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play original recording
                    IconButton(onClick = {
                        onPlayRecord()
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Play original record",
                            tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                    // Speak/TTS testing text
                    IconButton(onClick = {
                        onSpeak()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Listen voice clone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Delete
                    IconButton(onClick = {
                        onDelete()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete voice profile",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                Text(
                    text = profile.analysisData,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

