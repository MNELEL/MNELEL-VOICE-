package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceClonerViewModel
import com.example.service.AudioAnalysisResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AudioBlobAnalyzerScreen(viewModel: VoiceClonerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAnalyzing by viewModel.isGeminiAnalyzingAudio.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    val analysisResult by viewModel.audioAnalysisResult.collectAsState()

    var selectedSourceTab by remember { mutableStateOf(0) } // 0: הקלטה, 1: קובץ אחרון, 2: דוגמה מובנית
    
    val isRecordingReal by viewModel.isRecording.collectAsState()
    val isRecordingPausedReal by viewModel.isRecordingPaused.collectAsState()
    val recordingDurationSecReal by viewModel.recordingDurationSec.collectAsState()
    val liveAmplitudeReal by viewModel.liveAmplitude.collectAsState()
    val recordedFileReal by viewModel.recordedFile.collectAsState()

    val isPlayingRecorded by viewModel.isPlayingRecorded.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val playbackElapsedText by viewModel.playbackElapsedText.collectAsState()
    val playbackDurationText by viewModel.playbackDurationText.collectAsState()

    var profileName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("זכר") }
    var voiceDescription by remember { mutableStateOf("") }

    val scrollingAmplitudes = remember { mutableStateListOf<Float>() }
    LaunchedEffect(liveAmplitudeReal) {
        if (isRecordingReal && !isRecordingPausedReal) {
            scrollingAmplitudes.add(liveAmplitudeReal.coerceIn(0.05f, 1.0f))
            if (scrollingAmplitudes.size > 20) {
                scrollingAmplitudes.removeAt(0)
            }
        }
    }

    val recordedBlobBytes = remember(recordedFileReal) {
        if (recordedFileReal != null && recordedFileReal!!.exists()) {
            try {
                recordedFileReal!!.readBytes()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Demo raw bytes simulation
    val demoBlobBytes = remember {
        ByteArray(1000) { i -> (i % 128).toByte() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "מנתח שמע ואבחון קולי פונטי 🎙️",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ניתוח אקוסטי מתקדם של קובצי שמע (Audio Blobs) להפקת מדדי גובה צליל, רעשי רקע, ואיכות הגייה.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error message if any
        analysisError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                }
            }
        }

        // Step 1: Select or Capture Audio Blob
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "שלב 1: בחירת מקור השמע (Blob Source)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Tab selectors
                TabRow(
                    selectedTabIndex = selectedSourceTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedSourceTab == 0,
                        onClick = { selectedSourceTab = 0 },
                        text = { Text("הקלטה חדשה", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSourceTab == 1,
                        onClick = { selectedSourceTab = 1 },
                        text = { Text("דגימה אחרונה", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSourceTab == 2,
                        onClick = { selectedSourceTab = 2 },
                        text = { Text("קובץ דמו 🚀", fontSize = 12.sp) }
                    )
                }

                when (selectedSourceTab) {
                    0 -> {
                        // Recording section using real-time hardware recorder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRecordingReal) {
                                Text(
                                    text = if (isRecordingPausedReal) "הקלטה מושהית... ${recordingDurationSecReal} שניות" else "מקליט אודיו בזמן אמת... ${recordingDurationSecReal} שניות 🎙️",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                                // Realtime wave indicator
                                Row(
                                    modifier = Modifier.height(36.dp).fillMaxWidth(0.8f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    scrollingAmplitudes.forEach { amp ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 2.dp)
                                                .width(4.dp)
                                                .fillMaxHeight(amp)
                                                .background(if (isRecordingPausedReal) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pause/Resume button
                                    FilledTonalButton(
                                        onClick = {
                                            if (isRecordingPausedReal) {
                                                viewModel.resumeRecordVoice()
                                            } else {
                                                viewModel.pauseRecordVoice()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isRecordingPausedReal) Icons.Default.PlayArrow else Icons.Default.Menu,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isRecordingPausedReal) "המשך" else "השהה")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.stopRecordVoice()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("עצור ושמור")
                                    }
                                }
                            } else {
                                if (recordedBlobBytes != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                        Text("הקלטה שמורה בזיכרון (גודל: ${(recordedBlobBytes.size / 1024)} KB)", fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AudioPlayerControl(
                                        isPlaying = isPlayingRecorded,
                                        progress = playbackProgress,
                                        elapsedText = playbackElapsedText,
                                        durationText = playbackDurationText,
                                        onPlayPause = {
                                            if (isPlayingRecorded) {
                                                viewModel.stopRecordedFile()
                                            } else {
                                                viewModel.playRecordedFile()
                                            }
                                        },
                                        onStop = { viewModel.stopRecordedFile() },
                                        onSeek = { viewModel.seekPlayback(it) }
                                    )
                                } else {
                                    Text(
                                        "אנא הקלט דגימה של לפחות 5 שניות כדי להתחיל באבחון.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.startRecordVoice()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (recordedBlobBytes == null) "התחל הקלטת ה-Blob" else "הקלט מחדש")
                                }
                            }
                        }
                    }
                    1 -> {
                        // Last recorded file source
                        val recordedFile by viewModel.recordedFile.collectAsState()
                        if (recordedFile != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Text("קובץ שמע פעיל קיים במערכת:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("נתיב: ${recordedFile!!.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("גודל: ${(recordedFile!!.length() / 1024)} KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                AudioPlayerControl(
                                    isPlaying = isPlayingRecorded,
                                    progress = playbackProgress,
                                    elapsedText = playbackElapsedText,
                                    durationText = playbackDurationText,
                                    onPlayPause = {
                                        if (isPlayingRecorded) {
                                            viewModel.stopRecordedFile()
                                        } else {
                                            viewModel.playRecordedFile()
                                        }
                                    },
                                    onStop = { viewModel.stopRecordedFile() },
                                    onSeek = { viewModel.seekPlayback(it) }
                                )
                            }
                        } else {
                            Text(
                                "אין דגימת קול מוקלטת כרגע ביישום הראשי. אנא בצע הקלטה קודם, או השתמש במקור אחר.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    2 -> {
                        // Built-in demo blob
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("דגימת דמו קבועה של 12 שניות נטענה בהצלחה (ללא צורך בהקלטה מוקדמת).", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Trigger Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val blobToAnalyze = when (selectedSourceTab) {
                                0 -> recordedBlobBytes
                                1 -> {
                                    val f = viewModel.recordedFile.value
                                    if (f != null && f.exists()) f.readBytes() else null
                                }
                                2 -> demoBlobBytes
                                else -> null
                            }

                            if (blobToAnalyze != null) {
                                viewModel.analyzeAudioBlob(blobToAnalyze)
                            } else {
                                ToastHelper.show(context, "נא לספק קובץ או הקלטת שמע")
                            }
                        }
                    },
                    enabled = !isAnalyzing && (selectedSourceTab != 0 || recordedBlobBytes != null) && (selectedSourceTab != 1 || viewModel.recordedFile.value != null),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("מנתח באמצעות מנוע אקוסטי מקומי...")
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("בצע אנליזה קולית מקיפה 🧠")
                    }
                }
            }
        }

        // Step 2: Show Analysis Dashboard
        if (analysisResult != null) {
            val result = analysisResult!!

            Text(
                text = "שלב 2: לוח מחוונים ואבחון אקוסטי מקיף",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Overarching score circle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = (result.clarityScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.clarityScore}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "איכות כללית",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "סיכום אקוסטי ספקטרלי:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = result.overallSummary,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Grid of metric cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricDashboardCard(
                            title = "גובה קול (Pitch)",
                            value = "${result.estimatedPitchHz} Hz",
                            desc = result.pitchFrequencies,
                            icon = Icons.Default.Add,
                            accentColor = Color(0xFF3B82F6)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricDashboardCard(
                            title = "רעש רקע (SNR)",
                            value = "${result.estimatedSnrDb} dB",
                            desc = result.backgroundNoiseLevels,
                            icon = Icons.Default.Info,
                            accentColor = Color(0xFFEF4444)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricDashboardCard(
                            title = "הגייה ורהיטות",
                            value = "${result.pronunciationClarity}%",
                            desc = result.phoneticParameters,
                            icon = Icons.Default.CheckCircle,
                            accentColor = Color(0xFF10B981)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricDashboardCard(
                            title = "אינטונציה ומנגינה",
                            value = "${result.intonationScore}/100",
                            desc = "דירוג מנעד אינטונציה והתנגנות דיבור קולית טבעית.",
                            icon = Icons.Default.Star,
                            accentColor = Color(0xFF8B5CF6)
                        )
                    }
                }
            }

            // Deep visual insight section with canvas drawings
            var selectedInsightTab by remember { mutableStateOf(0) }
            val insightTabs = listOf("גרף מכ״ם (Radar) 🎯", "מפה ספקטרלית", "ניתוח רעשים")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(
                    selectedTabIndex = selectedInsightTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    insightTabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedInsightTab == idx,
                            onClick = { selectedInsightTab = idx },
                            text = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                when (selectedInsightTab) {
                    0 -> {
                        var showAsRadar by remember { mutableStateOf(true) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showAsRadar) "אבחון מדדים במפת מכ״ם" else "אבחון מדדים בעמודות",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FilledTonalButton(
                                        onClick = { showAsRadar = !showAsRadar },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showAsRadar) Icons.Default.Menu else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showAsRadar) "גרף עמודות" else "גרף מכ״ם", fontSize = 11.sp)
                                    }
                                }

                                if (showAsRadar) {
                                    VoicePhoneticRadarChart(result = result)
                                } else {
                                    VoicePhoneticBarChart(result = result)
                                }
                            }
                        }
                    }
                    1 -> {
                        // Drawing custom Pitch Frequency Line Chart using Canvas
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "מפת תדר קול (Pitch Contour Envelope)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        val points = 30
                                        val stepX = width / (points - 1)
                                        val path = androidx.compose.ui.graphics.Path()

                                        // Draw grid lines
                                        for (i in 1..3) {
                                            val y = height * (i.toFloat() / 4)
                                            drawLine(
                                                color = Color(0xFF334155),
                                                start = Offset(0f, y),
                                                end = Offset(width, y),
                                                strokeWidth = 1f
                                            )
                                        }

                                        // Generate visual pitch contour line
                                        val basePitchY = height * 0.5f
                                        for (i in 0 until points) {
                                            val x = i * stepX
                                            val angle = (i.toFloat() / points) * (3.1415f * 4f)
                                            // Combine waves for a voice pitch fluctuation simulation
                                            val wave = (sin(angle) * 30) + (cos(angle * 2.5f) * 15)
                                            val y = (basePitchY + wave).coerceIn(10f, height - 10f)

                                            if (i == 0) {
                                                path.moveTo(x, y)
                                            } else {
                                                path.lineTo(x, y)
                                            }
                                        }

                                        drawPath(
                                            path = path,
                                            color = Color(0xFF3B82F6),
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )

                                        // Draw filled area underneath the line
                                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                                            addPath(path)
                                            lineTo(width, height)
                                            lineTo(0f, height)
                                            close()
                                        }
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.3f), Color.Transparent)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Drawing custom Signal-to-Noise visual area diagram
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "חלוקת אנרגיית שמע לעומת רעשי רקע (SNR Spectrum)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        val stepX = width / 15f

                                        // Draw bars of signal (green) and noise (red) overlapping
                                        for (i in 0..15) {
                                            val x = i * stepX + (stepX * 0.1f)
                                            val signalHeight = height * (0.4f + 0.5f * sin(i.toFloat() * 0.5f).coerceIn(0f, 1f))
                                            val noiseHeight = height * (0.05f + 0.15f * cos(i.toFloat() * 0.8f).coerceIn(0f, 1f))

                                            // Draw Signal Bar
                                            drawRect(
                                                color = Color(0xFF10B981),
                                                topLeft = Offset(x, height - signalHeight),
                                                size = androidx.compose.ui.geometry.Size(stepX * 0.4f, signalHeight)
                                            )

                                            // Draw Noise Bar
                                            drawRect(
                                                color = Color(0xFFEF4444).copy(alpha = 0.7f),
                                                topLeft = Offset(x + (stepX * 0.4f), height - noiseHeight),
                                                size = androidx.compose.ui.geometry.Size(stepX * 0.3f, noiseHeight)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export report button
            Button(
                onClick = {
                    val reportJson = org.json.JSONObject().apply {
                        put("overallScore", result.clarityScore)
                        put("pitchHz", result.estimatedPitchHz)
                        put("snrDb", result.estimatedSnrDb)
                        put("summary", result.overallSummary)
                        put("phonetic", result.phoneticParameters)
                    }.toString(4)
                    
                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData.newPlainText("Voice Analysis Report", reportJson)
                    clipboardManager.setPrimaryClip(clipData)
                    ToastHelper.show(context, "דוח ה-JSON הועתק ללוח בהצלחה! 📋")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("העתק דוח אבחון מלא (JSON) ללוח 📋")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save analyzed signature as profile
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("שמירת חתימת הקול כפרופיל קול חדש במערכת 🗄️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    
                    Text(
                        "תוכל לשמור את תוצאות האבחון והניתוח האקוסטי כפרופיל קול משובט לשימוש עתידי במחולל הקול (TTS).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("שם פרופיל הקול (לדוגמה: אבא - קול משובט)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("מגדר:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        listOf("זכר", "נקבה", "אחר").forEach { g ->
                            val isSel = selectedGender == g
                            Button(
                                onClick = { selectedGender = g },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(g, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = voiceDescription,
                        onValueChange = { voiceDescription = it },
                        label = { Text("תיאור או הערות (לדוגמה: קול עמוק, אינטונציה רגועה)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Button(
                        onClick = {
                            if (profileName.isBlank()) {
                                ToastHelper.show(context, "אנא הזן שם לפרופיל הקול")
                            } else {
                                val savedFile = viewModel.recordedFile.value
                                val newProfile = com.example.data.VoiceProfile(
                                    name = profileName,
                                    gender = when(selectedGender) {
                                        "זכר" -> "Male"
                                        "נקבה" -> "Female"
                                        else -> "Other"
                                    },
                                    description = voiceDescription.ifBlank { "פרופיל שנוצר מאבחון אקוסטי" },
                                    audioPath = savedFile?.absolutePath,
                                    pitch = if (result.estimatedPitchHz < 160) "Deep" else if (result.estimatedPitchHz < 220) "Medium" else "High",
                                    tone = result.voiceToneAndStyle.ifBlank { "Warm" },
                                    vibe = "Professional",
                                    pace = "Medium",
                                    geminiVoiceName = "Custom Cloned",
                                    frequencyHz = result.estimatedPitchHz,
                                    clarityScore = result.clarityScore,
                                    pronunciationClarity = result.pronunciationClarity,
                                    intonationScore = result.intonationScore,
                                    breathPauseScore = result.breathPauseScore,
                                    distortionLevel = 100 - result.clarityScore,
                                    embedding = result.voicePrint.toByteArray(),
                                    isDraft = false
                                )
                                viewModel.saveProfile(newProfile)
                                ToastHelper.show(context, "פרופיל הקול '$profileName' נשמר בהצלחה לזיכרון המקומי! 💾")
                                profileName = ""
                                voiceDescription = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שמור פרופיל קול משובט במאגר 💾")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val draftName = profileName.ifBlank { "טיוטה - ${System.currentTimeMillis()}" }
                            val savedFile = viewModel.recordedFile.value
                            val newProfile = com.example.data.VoiceProfile(
                                name = draftName,
                                gender = when(selectedGender) {
                                    "זכר" -> "Male"
                                    "נקבה" -> "Female"
                                    else -> "Other"
                                },
                                description = voiceDescription.ifBlank { "טיוטה להמשך עריכה" },
                                audioPath = savedFile?.absolutePath,
                                pitch = if (result.estimatedPitchHz < 160) "Deep" else if (result.estimatedPitchHz < 220) "Medium" else "High",
                                tone = result.voiceToneAndStyle.ifBlank { "Warm" },
                                vibe = "Professional",
                                pace = "Medium",
                                geminiVoiceName = "Custom Cloned",
                                frequencyHz = result.estimatedPitchHz,
                                clarityScore = result.clarityScore,
                                pronunciationClarity = result.pronunciationClarity,
                                intonationScore = result.intonationScore,
                                breathPauseScore = result.breathPauseScore,
                                distortionLevel = 100 - result.clarityScore,
                                embedding = result.voicePrint.toByteArray(),
                                isDraft = true
                            )
                            viewModel.saveProfile(newProfile)
                            ToastHelper.show(context, "הטיוטה נשמרה בהצלחה! 📝")
                            profileName = ""
                            voiceDescription = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שמור כטיוטה להמשך עריכה 📝")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Saved Voice Profiles (Local database management)
        val savedProfiles by viewModel.allProfiles.collectAsState()
        val isPlayingProfileId by viewModel.isPlayingProfileId.collectAsState()
        
        Text(
            text = "ניהול קולות משובטים שמורים במאגר 🗄️",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (savedProfiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Text(
                    "אין עדיין פרופילי קול שמורים במאגר המקומי. בצע אבחון אקוסטי ושמור אותו כדי לנהל את הקולות כאן.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                savedProfiles.take(5).forEach { profile ->
                    var isEditingName by remember { mutableStateOf(false) }
                    var editedName by remember { mutableStateOf(profile.name) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEditingName) {
                                    OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        if (editedName.isNotBlank()) {
                                            viewModel.renameProfile(profile.id, editedName)
                                            isEditingName = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "שמור", tint = Color(0xFF10B981))
                                    }
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (profile.isDraft) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("טיוטה", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                        }
                                        Text(profile.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("תדר ממוצע: ${profile.frequencyHz}Hz | איכות: ${profile.clarityScore}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    Row {
                                        IconButton(onClick = { isEditingName = true }) {
                                            Icon(Icons.Default.Settings, contentDescription = "ערוך שם", modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                            Icon(Icons.Default.Warning, contentDescription = "מחק קול", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            
                            if (profile.audioPath != null) {
                                val isThisPlaying = isPlayingProfileId == profile.id
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (isThisPlaying) {
                                                viewModel.stopProfileSample()
                                            } else {
                                                viewModel.playProfileSample(profile)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Menu else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isThisPlaying) "עצור דגימה" else "השמע דגימה", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                if (savedProfiles.size > 5) {
                    Text(
                        "ומעוד ${savedProfiles.size - 5} פרופילי קול השמורים במערכת שתוכל לראות בגלריה הראשית.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricDashboardCard(
    title: String,
    value: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )

            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 15.sp
            )
        }
    }
}

// Simple Toast helper
object ToastHelper {
    fun show(context: android.content.Context, msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun VoicePhoneticRadarChart(
    result: AudioAnalysisResult,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    // Data points
    val metrics = listOf(
        "בהירות קול" to result.clarityScore.toFloat(),
        "דיוק הגייה" to result.pronunciationClarity.toFloat(),
        "אינטונציה" to result.intonationScore.toFloat(),
        "נשימה וקצב" to result.breathPauseScore.toFloat(),
        "אות לרעש" to (result.estimatedSnrDb * 2).toFloat().coerceIn(10f, 100f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = (size.height / 2) * 0.7f
            val numPoints = metrics.size

            // Draw 5 grid web rings (20%, 40%, 60%, 80%, 100%)
            for (ring in 1..5) {
                val level = ring / 5f
                val ringRadius = maxRadius * level
                val ringPath = androidx.compose.ui.graphics.Path()
                for (i in 0 until numPoints) {
                    val angle = (i * 2 * Math.PI / numPoints) - Math.PI / 2
                    val x = (center.x + ringRadius * cos(angle)).toFloat()
                    val y = (center.y + ringRadius * sin(angle)).toFloat()
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(
                    path = ringPath,
                    color = gridColor,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw axes from center to 100% outer ring
            for (i in 0 until numPoints) {
                val angle = (i * 2 * Math.PI / numPoints) - Math.PI / 2
                val targetX = (center.x + maxRadius * cos(angle)).toFloat()
                val targetY = (center.y + maxRadius * sin(angle)).toFloat()
                drawLine(
                    color = gridColor,
                    start = center,
                    end = Offset(targetX, targetY),
                    strokeWidth = 1.5f.dp.toPx()
                )

                // Draw labels using native canvas for clean text rendering with specific alignment
                val labelOffsetRadius = maxRadius + 22.dp.toPx()
                val labelX = (center.x + labelOffsetRadius * cos(angle)).toFloat()
                val labelY = (center.y + labelOffsetRadius * sin(angle)).toFloat()

                // Draw Text
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(density) { 12.sp.toPx() }
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                
                // Adjust Y position slightly to center text vertically based on angle
                val adjustedY = if (sin(angle) > 0.1f) labelY + 12f else if (sin(angle) < -0.1f) labelY - 5f else labelY + 5f

                drawContext.canvas.nativeCanvas.drawText(
                    metrics[i].first,
                    labelX,
                    adjustedY,
                    paint
                )
            }

            // Draw actual metrics area
            val valuePath = androidx.compose.ui.graphics.Path()
            val pointsList = mutableListOf<Offset>()
            for (i in 0 until numPoints) {
                val value = metrics[i].second
                val angle = (i * 2 * Math.PI / numPoints) - Math.PI / 2
                val valueRadius = maxRadius * (value / 100f)
                val x = (center.x + valueRadius * cos(angle)).toFloat()
                val y = (center.y + valueRadius * sin(angle)).toFloat()
                val point = Offset(x, y)
                pointsList.add(point)
                if (i == 0) valuePath.moveTo(x, y) else valuePath.lineTo(x, y)
            }
            valuePath.close()

            // Draw filled metrics shape
            drawPath(
                path = valuePath,
                color = primaryColor.copy(alpha = 0.35f)
            )

            // Draw shape stroke
            drawPath(
                path = valuePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw dots at each metric value point
            pointsList.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun VoicePhoneticBarChart(
    result: AudioAnalysisResult,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val metrics = listOf(
        "בהירות קול" to result.clarityScore,
        "דיוק הגייה" to result.pronunciationClarity,
        "אינטונציה" to result.intonationScore,
        "נשימה וקצב" to result.breathPauseScore,
        "אות לרעש" to (result.estimatedSnrDb * 2).coerceIn(10, 100)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        metrics.forEach { (label, score) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$score%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                }

                // Custom bar with gradient and rounded corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(score / 100f)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(primaryColor, secondaryColor)
                                )
                            )
                    )
                }
            }
        }
    }
}
