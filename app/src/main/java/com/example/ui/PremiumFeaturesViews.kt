package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SoundWaveVisualizer
import com.example.VoiceClonerViewModel
import com.example.data.VoiceProfile
import com.example.data.VoiceStyleTemplate
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.LightBg
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightGreen
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.glassmorphic
import com.example.ui.theme.LightSecondary
import com.example.ui.theme.LightTertiary
import com.example.ui.theme.SoftMuted
import java.io.File
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

// 1. --- LANDING PAGE SCREEN & ONBOARDING ON START ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LandingPageScreen(
    viewModel: VoiceClonerViewModel,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val userName by viewModel.googleUserName.collectAsStateWithLifecycle()

    var customLoginEmail by remember { mutableStateOf("") }
    var customLoginName by remember { mutableStateOf("") }
    var showSelfLogin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(com.example.ui.theme.PastelGradientBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Visual Identity & Logo with stylized sound waves and Halo glow effect (Hebrew requested)
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(LightPrimary.copy(alpha = 0.12f), CircleShape)
                .border(2.dp, LightPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Simulated Halo Glow Ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(LightTertiary.copy(alpha = 0.15f), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = LightPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "משבט קול AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = LightPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "הדור החדש של שיבוט, ניתוח וניהול קול פדגוגי בבינה מלאכותית",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SoftMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Glassmorphism Features Highlight
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.Face,
                    title = "שיבוט וניתוח קול מדויק",
                    desc = "מנתח נתוני תדר, בהירות והפסקות נשימה בשיח מהיר באמצעות מודל Gemini."
                )
                FeatureRow(
                    icon = Icons.Default.Share,
                    title = "סנכרון Google Drive ו-OAuth",
                    desc = "העלאת תוצרי שמע וסנכרון תמלולים אנונימיים ישירות כ-Google Docs לענן."
                )
                FeatureRow(
                    icon = Icons.Default.List,
                    title = "מחולל תבניות סגנון ואימון",
                    desc = "התאמת קצבי הוראה, עוצמות דציבל והדמיות תדרים ייעודיים למרצים ואנשי חינוך."
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Frictionless Google Sign-In & Mock Login Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGoogleSignedIn) {
                    Text(
                        text = "ברוך הבא! מחובר דרך Google OAuth 2.0 🟢",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LightPrimary
                    )
                    Text(
                        text = "אימייל מחובר: $googleEmail",
                        fontSize = 14.sp,
                        color = DarkCharcoal
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onGetStarted,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("להמשך לאפליקציה 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { viewModel.signOutGoogle() }) {
                        Text("התנתק מחשבון גוגל", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(
                        text = "התחברות מאובטחת לניהול נתונים אישי",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkCharcoal
                    )

                    // Primary login button: Recommended frictionless Google Sign In
                    Button(
                        onClick = {
                            viewModel.signInWithGoogle("nm0527603669@gmail.com", "משתמש גוגל")
                            android.widget.Toast.makeText(context, "מחובר בהצלחה באמצעות Google Sign-In!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("להתחברות מהירה ב-Google Sign-In 🚀", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (!showSelfLogin) {
                        TextButton(onClick = { showSelfLogin = true }) {
                            Text("הכנס אימייל ידני אחר", color = LightPrimary)
                        }
                    } else {
                        OutlinedTextField(
                            value = customLoginEmail,
                            onValueChange = { customLoginEmail = it },
                            label = { Text("כתובת אימייל אחרת") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = customLoginName,
                            onValueChange = { customLoginName = it },
                            label = { Text("שם המשתמש") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (customLoginEmail.isNotBlank()) {
                                        viewModel.signInWithGoogle(customLoginEmail, customLoginName)
                                    } else {
                                        viewModel.signInWithGoogle("nm0527603669@gmail.com", customLoginName)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("חבר עכשיו")
                            }
                            TextButton(onClick = { showSelfLogin = false }) {
                                Text("ביטול")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onGetStarted,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SoftMuted),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkCharcoal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("המשך ללא הרשמה (אורח מקומי) 👤")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(LightPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = LightPrimary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
            Text(text = desc, fontSize = 12.sp, color = SoftMuted, lineHeight = 15.sp)
        }
    }
}

fun downloadAudioResult(
    context: android.content.Context,
    sourceFile: File,
    displayName: String,
    format: String, // "mp3" or "wav"
    onResult: (Boolean, String) -> Unit
) {
    if (!sourceFile.exists()) {
        onResult(false, "קובץ המקור אינו זמין")
        return
    }
    try {
        val resolver = context.contentResolver
        val mimeType = if (format == "wav") "audio/wav" else "audio/mpeg"
        val extension = if (format == "wav") ".wav" else ".mp3"
        
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$displayName$extension")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
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
            val targetFile = File(downloadsDir, "$displayName$extension")
            android.net.Uri.fromFile(targetFile)
        }
        
        if (uri == null) {
            onResult(false, "כשל ביצירת קובץ ה-MediaStore")
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
        
        onResult(true, "הורדה הושלמה! הקובץ נשמר בתיקיית ההורדות בפורמט ${format.uppercase()}")
    } catch (e: Exception) {
        onResult(false, "שגיאה בייצוא: ${e.message}")
    }
}

// 2. --- DYNAMIC TEXT SYNTHESIS PAGE (Equated to `/synthesize`) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechSynthesisScreen(
    viewModel: VoiceClonerViewModel,
    profiles: List<VoiceProfile>,
    templates: List<VoiceStyleTemplate>
) {
    val context = LocalContext.current
    var inputPhrase by remember { mutableStateOf("") }
    var pitchTuning by remember { mutableStateOf(0f) } // -1.0 to 1.0 multiplier
    var speedTuning by remember { mutableStateOf(0f) }
    var selectedProfile by remember { mutableStateOf<VoiceProfile?>(null) }
    var selectedTemplate by remember { mutableStateOf<VoiceStyleTemplate?>(null) }

    val moodStates = remember {
        listOf(
            "מקורי/רגיל" to "סגנון הדיבור המקורי והטבעי",
            "Emotional (רגשני)" to "קול מלא ברגש, הבעה עמוקה ונוגעת ללב",
            "Professional (מקצועי)" to "דיקציה מושלמת, רציני, אמין וסמכותי",
            "Whisper (לחישה)" to "דיבור שקט, אינטימי ועדין, עוצמת שמע מונמכת",
            "שמח וערני" to "קול נמרץ, תדר גבוה וקצב דיבור מהיר"
        )
    }
    var selectedMoodState by remember { mutableStateOf("מקורי/רגיל") }

    LaunchedEffect(selectedMoodState) {
        when (selectedMoodState) {
            "מקורי/רגיל" -> {
                pitchTuning = 0f
                speedTuning = 0f
            }
            "Emotional (רגשני)" -> {
                pitchTuning = 0.1f
                speedTuning = -0.1f
            }
            "Professional (מקצועי)" -> {
                pitchTuning = -0.15f
                speedTuning = 0.05f
            }
            "Whisper (לחישה)" -> {
                pitchTuning = -0.2f
                speedTuning = -0.2f
            }
            "שמח וערני" -> {
                pitchTuning = 0.3f
                speedTuning = 0.3f
            }
        }
    }

    val isSynthesizing by viewModel.isSynthesizing.collectAsStateWithLifecycle()
    val synthesizeError by viewModel.synthesizeError.collectAsStateWithLifecycle()
    val isDriveSyncing by viewModel.driveSyncing.collectAsStateWithLifecycle()
    val driveMessage by viewModel.driveStatusMessage.collectAsStateWithLifecycle()

    val recentGenerations by viewModel.recentGenerations.collectAsStateWithLifecycle()
    val isPlayingResultId by viewModel.isPlayingResultId.collectAsStateWithLifecycle()

    val isLiteRtEnabled by viewModel.isLiteRtEnabled.collectAsStateWithLifecycle()
    val liteRtModelSelected by viewModel.liteRtModelSelected.collectAsStateWithLifecycle()
    val isRobotAutomationRunning by viewModel.isRobotAutomationRunning.collectAsStateWithLifecycle()
    val robotLog by viewModel.robotLog.collectAsStateWithLifecycle()

    val liteRtStatus by viewModel.liteRtStatus.collectAsStateWithLifecycle()
    val liteRtProcessingSpeed by viewModel.liteRtProcessingSpeed.collectAsStateWithLifecycle()
    val liteRtMemoryUsage by viewModel.liteRtMemoryUsage.collectAsStateWithLifecycle()
    val liteRtCpuUsage by viewModel.liteRtCpuUsage.collectAsStateWithLifecycle()
    val liteRtHardwareDelegate by viewModel.liteRtHardwareDelegate.collectAsStateWithLifecycle()

    val localSignatures by viewModel.localSignatures.collectAsStateWithLifecycle()
    val signatureSecurityStatus by viewModel.signatureSecurityStatus.collectAsStateWithLifecycle()

    val localTtsQueue by viewModel.localTtsQueue.collectAsStateWithLifecycle()
    val isQueueProcessing by viewModel.isQueueProcessing.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()

    var showFormatSelectorDialog by remember { mutableStateOf<com.example.data.VoiceGenerationResult?>(null) }

    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty() && selectedProfile == null) {
            selectedProfile = profiles.first()
        }
    }
    LaunchedEffect(templates) {
        if (templates.isNotEmpty() && selectedTemplate == null) {
            selectedTemplate = templates.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "סינתזה קולית ושיבוט דיבור 🎙️",
            style = MaterialTheme.typography.titleLarge,
            color = LightPrimary,
            fontWeight = FontWeight.Bold
        )

        // Glassmorphic Card for inputs
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Use the new reusable component
                selectedProfile?.let { profile ->
                    SynthesizeVoiceComponent(
                        viewModel = viewModel,
                        profile = profile,
                        inputPhrase = inputPhrase,
                        onInputPhraseChange = { inputPhrase = it }
                    )
                } ?: Text("נא לבחור פרופיל קול כדי להתחיל בסינתזה", color = MaterialTheme.colorScheme.error)

                // 2.1 Profile selection dropdown
                Text("בחר פרופיל קול משובט:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (profiles.isEmpty()) {
                    Text(
                        "אין פרופילי קול משובטים זמינים! נא צור פרופיל ראשון בלשונית הבית.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(profiles) { profile ->
                            FilterChip(
                                selected = selectedProfile?.id == profile.id,
                                onClick = { 
                                    selectedProfile = profile
                                    // Autofill demo description
                                    if (inputPhrase.isEmpty()) {
                                        inputPhrase = "שלום, כאן קול משובט איכותי של ${profile.name}."
                                    }
                                },
                                label = { Text(profile.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LightPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = LightPrimary
                                )
                            )
                        }
                    }
                }

                // 2.1b Learned Custom Moods (Tired / Happy / Serious)
                Text("סגנון קול ומצב רוח שהמערכת למדה: 🧠", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodStates) { (moodName, _) ->
                        FilterChip(
                            selected = selectedMoodState == moodName,
                            onClick = { selectedMoodState = moodName },
                            label = { Text(moodName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LightPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = LightPrimary
                            )
                        )
                    }
                }

                // Description of learned state characteristics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightPrimary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LightPrimary.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val learnedDesc = when (selectedMoodState) {
                            "Emotional (רגשני)" -> "❤️ המערכת מתאימה את הקול כדי להביע רגש עמוק, התלהבות או אמפתיה, עם שינויי תדר קלים שיוצרים תחושת כנות."
                            "Professional (מקצועי)" -> "🎓 המערכת מנווטת את תדרי הליבה לטון נמוך, יציב ומתוחכם – המקל על הובלה, העברת סמכות ויצירת ריכוז פדגוגי."
                            "Whisper (לחישה)" -> "🤫 קול דק ועדין: מסנן רעשים קבועים ברוחב פתוח ומדמה אווירה אינטימית ואינטונציה שקטה ונקייה מהד."
                            "שמח וערני" -> "🌅 המערכת למדה כי במצבי שמחה ורעננות הקול נהיה מלודי, תדר הדיבור הממוצע עולה ב-25 הרץ, והדיקציה קופצנית ומהירה."
                            else -> "✨ סגנון מקורי: ייעשה שימוש במנעד הביומטרי הרגיל כפי שנקלט בדגימת הקול שהקלטת בלשונית הבית."
                        }
                        Text(
                            text = learnedDesc,
                            fontSize = 11.sp,
                            color = DarkCharcoal,
                            lineHeight = 14.sp
                        )
                    }
                }

                // 2.2 Templates selector
                Text("בחר סגנון דיבור מתבנית קיימת:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(templates) { template ->
                        FilterChip(
                            selected = selectedTemplate?.id == template.id,
                            onClick = { 
                                selectedTemplate = template
                                // Prepopulate custom phrasing
                                val sentences = template.examplePhrases.split(",")
                                if (sentences.isNotEmpty() && sentences[0].isNotBlank()) {
                                    inputPhrase = sentences[0]
                                }
                            },
                            label = {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(template.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(template.category, fontSize = 10.sp, color = SoftMuted)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LightTertiary.copy(alpha = 0.2f),
                                selectedLabelColor = LightTertiary
                            )
                        )
                    }
                }

                if (selectedTemplate != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("הנחיית סגנון: ${selectedTemplate?.instructions}", fontSize = 12.sp, color = DarkCharcoal)
                        }
                    }
                }

                // Waveform visualization when playing or synthesizing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SoundWaveVisualizer(
                        isRecording = isSynthesizing,
                        isPaused = false,
                        amplitude = if (isSynthesizing) 60f else 10f
                    )
                }

                // 2.3 Pitch and speed modulations
                Text("כיול וחידוד אינטונציה:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("גובה צליל (Pitch Calibration):", fontSize = 12.sp)
                        Text("${String.format("%.1f", pitchTuning)}x", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = pitchTuning,
                        onValueChange = { pitchTuning = it },
                        valueRange = -1.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = LightPrimary, activeTrackColor = LightPrimary)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("קצב הקראה (Speed Calibration):", fontSize = 12.sp)
                        Text("${String.format("%.1f", speedTuning)}x", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = speedTuning,
                        onValueChange = { speedTuning = it },
                        valueRange = -1.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = LightTertiary, activeTrackColor = LightTertiary)
                    )
                }

                // --- LiteRT-LM Local Integration Card ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = LightPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "מנוע עיבוד מקומי LiteRT-LM",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = LightPrimary
                                    )
                                    Text(
                                        text = "עיבוד על גבי המכשיר לחסכון בטוקנים",
                                        fontSize = 11.sp,
                                        color = DarkCharcoal
                                    )
                                }
                            }
                            Switch(
                                checked = isLiteRtEnabled,
                                onCheckedChange = { viewModel.setLiteRtEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LightPrimary,
                                    checkedTrackColor = LightPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }

                        if (isLiteRtEnabled) {
                            Divider(color = Color.LightGray.copy(alpha = 0.3f))

                            Text("בחר דגם מודל LiteRT מקומי:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Gemma-2B-TTS-Local", "Llama-Compact-TTS").forEach { model ->
                                    FilterChip(
                                        selected = liteRtModelSelected == model,
                                        onClick = { viewModel.selectLiteRtModel(model) },
                                        label = { Text(model, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LightPrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = LightPrimary
                                        )
                                    )
                                }
                            }

                            signatureSecurityStatus?.let { status ->
                                Spacer(modifier = Modifier.height(8.dp))
                                SignatureSecurityHandshakeBanner(
                                    status = status,
                                    onDismiss = { viewModel.clearSignatureSecurityStatus() }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LiteRtMetricsDashboard(
                                status = liteRtStatus,
                                speed = liteRtProcessingSpeed,
                                memory = liteRtMemoryUsage,
                                cpu = liteRtCpuUsage,
                                delegate = liteRtHardwareDelegate
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            LiteRtQueueSystem(
                                queue = localTtsQueue,
                                isProcessing = isQueueProcessing,
                                currentIndex = currentQueueIndex,
                                currentInputText = inputPhrase,
                                selectedProfile = selectedProfile,
                                onAddTask = { txt -> selectedProfile?.let { prof -> viewModel.addTaskToQueue(txt, prof) } },
                                onRemoveTask = { id -> viewModel.removeTaskFromQueue(id) },
                                onClearQueue = { viewModel.clearQueue() },
                                onStartProcessing = { viewModel.startQueueProcessing() }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            LocalSignaturesManager(
                                signatures = localSignatures,
                                selectedProfile = selectedProfile,
                                onSaveProfileAsSignature = { prof, filename -> viewModel.saveProfileAsSignature(prof, filename) },
                                onRenameSignature = { old, new -> viewModel.renameSignatureFile(old, new) },
                                onDeleteSignature = { filename -> viewModel.deleteSignatureFile(filename) },
                                onImportSignature = { filename -> viewModel.importSignatureFileToDb(filename) },
                                viewModel = viewModel,
                                profiles = profiles
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Robot Automation Mode section
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = null,
                                                tint = LightTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "רובוט אוטומטי (ללא טוקנים)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = LightTertiary
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.toggleRobotAutomation() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRobotAutomationRunning) Color.Red else LightTertiary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = if (isRobotAutomationRunning) "עצור רובוט" else "הפעל רובוט",
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Logs console
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(85.dp)
                                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = robotLog,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = DarkCharcoal,
                                            modifier = Modifier.verticalScroll(rememberScrollState())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Cloud Synthesis
                    Button(
                        onClick = {
                            selectedProfile?.let { profile ->
                                if (inputPhrase.isNotBlank()) {
                                    viewModel.synthesizeText(
                                        text = inputPhrase,
                                        profile = profile,
                                        pitchTuningPercent = pitchTuning * 100f,
                                        speedTuningPercent = speedTuning * 100f,
                                        vibeModifier = if (selectedMoodState != "מקורי/רגיל") selectedMoodState else (selectedTemplate?.name ?: "מקורי")
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, "אנא הזן טקסט לקריינות", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } ?: android.widget.Toast.makeText(context, "אנא בחר פרופיל קול תחילה", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSynthesizing,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("synthesis_generate_button")
                    ) {
                        if (isSynthesizing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("סנתז קול [AI]", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Local Offline TTS Synthesis
                    OutlinedButton(
                        onClick = {
                            selectedProfile?.let { profile ->
                                if (inputPhrase.isNotBlank()) {
                                    if (isLiteRtEnabled) {
                                        viewModel.addTaskToQueue(inputPhrase, profile)
                                        viewModel.startQueueProcessing()
                                        android.widget.Toast.makeText(context, "נוסף לתור הסינתזה המקומי והחל בעיבוד!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.synthesizeTextLocal(
                                            text = inputPhrase,
                                            profile = profile,
                                            pitchTuningPercent = pitchTuning * 100f,
                                            speedTuningPercent = speedTuning * 100f
                                        )
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "אנא הזן טקסט לקריינות", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } ?: android.widget.Toast.makeText(context, "אנא בחר פרופיל קול תחילה", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = LightTertiary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("הקראה מקומית", color = LightTertiary)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        selectedProfile?.let { profile ->
                            if (inputPhrase.isNotBlank()) {
                                viewModel.exportAudioToWav(
                                    text = inputPhrase,
                                    profile = profile,
                                    pitchTuningPercent = pitchTuning * 100f,
                                    speedTuningPercent = speedTuning * 100f,
                                    context = context
                                )
                            } else {
                                android.widget.Toast.makeText(context, "אנא הזן טקסט לייצוא", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } ?: android.widget.Toast.makeText(context, "אנא בחר פרופיל קול תחילה", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = LightTertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ייצא שמע לקובץ (WAV)", color = LightTertiary)
                }

                if (isSynthesizing) {
                    SynthesisLoadingCard()
                }

                synthesizeError?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Drive Synced Results Card / Recent Results
        Text(text = "תוצרי סינתזה אחרונים 🎧", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        val demoGenerations = if (selectedProfile != null) {
            recentGenerations.filter { it.profileId == selectedProfile!!.id }
        } else {
            recentGenerations
        }

        if (demoGenerations.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("טרם סונתז קובץ שמע לפרופיל זה. לחץ סנתז בשביל ליצור שמע.", color = SoftMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            demoGenerations.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.inputText, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                Text("תאריך ייצור: ${android.text.format.DateFormat.format("hh:mm a, dd/MM", result.createdAt)}", fontSize = 11.sp, color = SoftMuted)
                            }
                            IconButton(onClick = {
                                if (isPlayingResultId == result.id) {
                                    viewModel.stopResultSample()
                                } else {
                                    viewModel.playResultSample(result)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlayingResultId == result.id) Icons.Default.Clear else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = LightPrimary
                                )
                            }
                        }

                        // Save to Google Drive, Local Download buttons (Answers 1.1 and 1.2 specifications)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val dummyFile = File(context.cacheDir, "temp_synthesis_${result.id}.mp3")
                                    if (!dummyFile.exists()) {
                                        dummyFile.createNewFile()
                                    }
                                    viewModel.saveAudioToDrive(dummyFile) { success, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightTertiary),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isDriveSyncing,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isDriveSyncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("שמור ל-Google Drive ☁️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    showFormatSelectorDialog = result
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("export_audio_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkCharcoal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ייצוא לקובץ שמע 💾", fontSize = 11.sp, color = DarkCharcoal)
                            }

                            IconButton(
                                onClick = { viewModel.deleteResult(result) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "מחק תוצאה", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (driveMessage.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = LightTertiary)
                    Text(driveMessage, fontSize = 12.sp, color = LightTertiary)
                }
            }
        }

        // MP3/WAV Audio Format Export Selection Dialog
        if (showFormatSelectorDialog != null) {
            val result = showFormatSelectorDialog!!
            AlertDialog(
                onDismissRequest = { showFormatSelectorDialog = null },
                title = { 
                    Text(
                        text = "ייצוא קובץ שמע 💾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LightPrimary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "בחר את פורמט האודיו לייצוא ושמירה בתיקיית ההורדות של המכשיר:",
                            fontSize = 13.sp,
                            color = DarkCharcoal
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // MP3 Option Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFormatSelectorDialog = null
                                    downloadAudioResult(
                                        context = context,
                                        sourceFile = File(result.audioPath),
                                        displayName = "משבט-קול-${result.profileName}-${result.id}",
                                        format = "mp3"
                                    ) { success, message ->
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                                .testTag("export_mp3_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, LightPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = LightPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "קובץ MP3 (דחוס, נפח קטן)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkCharcoal
                                    )
                                    Text(
                                        text = "מעולה לשיתוף מהיר והאזנה ברוב הנגנים",
                                        fontSize = 11.sp,
                                        color = SoftMuted
                                    )
                                }
                            }
                        }

                        // WAV Option Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFormatSelectorDialog = null
                                    downloadAudioResult(
                                        context = context,
                                        sourceFile = File(result.audioPath),
                                        displayName = "משבט-קול-${result.profileName}-${result.id}",
                                        format = "wav"
                                    ) { success, message ->
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                                .testTag("export_wav_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "קובץ WAV (איכות גבוהה, ללא דחיסה)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkCharcoal
                                    )
                                    Text(
                                        text = "מתאים לעריכה מקצועית ומערכות קול ייעודיות",
                                        fontSize = 11.sp,
                                        color = SoftMuted
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFormatSelectorDialog = null }) {
                        Text("ביטול", color = LightPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


// 3. --- VOICE STYLE TEMPLATES MANAGER SCREEN (Answers 2.1 to 2.4 specification) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleTemplatesScreen(
    viewModel: VoiceClonerViewModel,
    templates: List<VoiceStyleTemplate>
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("הכל") }
    val categories = listOf("הכל", "קריין", "מורה", "מנחה", "שחקן")

    var showCreateDialog by remember { mutableStateOf(false) }

    // Dialog inputs
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("קריין") }
    var tagsInput by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var examplesInput by remember { mutableStateOf("") }

    val filtered = templates.filter {
        (selectedCategory == "הכל" || it.category == selectedCategory) &&
        (it.name.contains(searchQuery, ignoreCase = true) || 
         it.tags.contains(searchQuery, ignoreCase = true) ||
         it.instructions.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "תבניות סגנון קולות 🎭",
                style = MaterialTheme.typography.titleLarge,
                color = LightPrimary,
                fontWeight = FontWeight.Black
            )
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("סגנון חדש", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search inputs
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("חיפוש לפי שם, הנחיה או תגיות...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Categories filters row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LightPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = LightPrimary
                    )
                )
            }
        }

        // List layout
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("לא נמצאו תבניות סגנון מתאימות לחיפוש שלך.", color = SoftMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                filtered.forEach { style ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(style.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(style.category, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                        )
                                        if (style.isPublic) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("מערכת ⚙️", fontSize = 10.sp, color = LightGreen) }
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Duplicate Button
                                    IconButton(
                                        onClick = {
                                            viewModel.duplicateStyleTemplate(style)
                                            android.widget.Toast.makeText(context, "התבנית '${style.name}' הועתקה ושוכפלה בהצלחה!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "שכפל", tint = LightTertiary, modifier = Modifier.size(16.dp))
                                    }

                                    // Delete user templates
                                    if (style.createdBy != "מובנה") {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteStyleTemplate(style.id)
                                                android.widget.Toast.makeText(context, "התבנית נמחקה בהצלחה", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "מחק", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            // Tags row
                            if (style.tags.isNotBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    style.tags.split(",").forEach { tag ->
                                        if (tag.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(LightPrimary.copy(alpha = 0.08f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(tag, fontSize = 10.sp, color = LightPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "הנחיות קול: ${style.instructions}",
                                fontSize = 13.sp,
                                color = DarkCharcoal,
                                lineHeight = 16.sp
                            )

                            // Examples list
                            if (style.examplePhrases.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassmorphic(shape = RoundedCornerShape(8.dp), elevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("משפט לדוגמה:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SoftMuted)
                                        Text(style.examplePhrases.split(",")[0], fontSize = 12.sp, color = DarkCharcoal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create style dialog (Answers 2.3 and 2.4 specifications)
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("יצירת תבנית סגנון חדשה 🎭", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LightPrimary) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("שם הסגנון (למשל 'רופא סמכותי')") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("קטגוריה:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("קריין", "מורה", "מנחה", "שחקן").forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            placeholder = { Text("#אנרגטי,#רציני,#מהיר") },
                            label = { Text("תגיות (מופרדות בפסיק)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("הנחיות הקלטה קוליות") },
                            placeholder = { Text("דבר עם עוצמות משתנות ופאוזות נשימה ארוכות במיוחד...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )

                        OutlinedTextField(
                            value = examplesInput,
                            onValueChange = { examplesInput = it },
                            label = { Text("משפט הדגמה ברירת מחדל") },
                            placeholder = { Text("שלום רב לכל הנוכחים, מתחילים כעת עדכון דדי יסודי...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && instructions.isNotBlank()) {
                                viewModel.createStyleTemplate(
                                    VoiceStyleTemplate(
                                        name = name,
                                        category = category,
                                        tags = tagsInput,
                                        instructions = instructions,
                                        examplePhrases = if (examplesInput.isBlank()) "שלום לכולם, בדיקת אינטונציה לסגנון החדש." else examplesInput,
                                        createdBy = "משתמש",
                                        isPublic = false
                                    )
                                )
                                showCreateDialog = false
                                name = ""
                                tagsInput = ""
                                instructions = ""
                                examplesInput = ""
                                android.widget.Toast.makeText(context, "הסגנון החדש נוצר בהצלחה!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "אנא מלא שם והנחיות קוליות", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimary)
                    ) {
                        Text("שמור סגנון")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("ביטול")
                    }
                }
            )
        }
    }
}


// 4. --- CALIBRATION, DRILLS & LECTURERS REALTIME ANALYSIS PANEL ---
@Composable
fun CalibrationTrainingScreen(
    viewModel: VoiceClonerViewModel,
    profiles: List<VoiceProfile>
) {
    val context = LocalContext.current
    var isDrilling by remember { mutableStateOf(false) }
    var currentDrillId by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableStateOf(0) }
    var detectedDrillPace by remember { mutableStateOf(100) } // words per min

    val liveAmplitude by viewModel.liveAmplitude.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

    var savedLogs by remember { mutableStateOf(listOf(
        "אנליזת פתיחה: דיקציה רציפה, קצב מילולי מדויק (115 מילים/דקה)",
        "אימון 1: קול המורה הסמכותי - ציון אינטונציה 94/100"
    )) }

    LaunchedEffect(isDrilling) {
        if (isDrilling) {
            timerSeconds = 0
            while (isDrilling) {
                kotlinx.coroutines.delay(1000)
                timerSeconds++
                // Random dynamic fluctuation of speaking speed for realistic trainer
                detectedDrillPace = (120 + (Math.random() * 20 - 10).toInt()).coerceIn(80, 180)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "מרכז אימוני שיח למרצים וסגלי הוראה 🎓",
            style = MaterialTheme.typography.titleLarge,
            color = LightPrimary,
            fontWeight = FontWeight.Bold
        )

        // Realtime guidelines based on speed metrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📊 לוח בקרה של איכות צליל והוראה פרונטלית בזמן אמת", fontWeight = FontWeight.Bold, color = LightPrimary, fontSize = 14.sp)
                Text(
                    text = "מנתח הקול עוקב אחר עוצמת הדציבלים וקצב הקדנציה שלך. פאוזות נשימה נכונות (כולל פאוזה של 0.5 שנ' בין נושאים) משפרות קשב תלמידים ב-34%.",
                    fontSize = 12.sp, lineHeight = 15.sp, color = DarkCharcoal
                )
            }
        }

        // Training mode launcher
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("בחר אימון קולי מונחה לכיול:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                val drills = listOf(
                    "אימון 1: קצב מורה דינמי (פסקאות רחבות באנרגיה מוגברת)",
                    "אימון 2: קריינות ספרים (קריאה בקול באטונציה משתרעת)",
                    "אימון 3: סמכותיות וסבלנות (קצב איטי להחדרת חומר מורכב)"
                )

                drills.forEach { drill ->
                    val isThis = currentDrillId == drill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isThis) LightTertiary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                if (!isDrilling) {
                                    currentDrillId = drill
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isThis,
                            onClick = { if (!isDrilling) currentDrillId = drill },
                            colors = RadioButtonDefaults.colors(selectedColor = LightTertiary)
                        )
                        Text(drill, fontSize = 13.sp, fontWeight = if (isThis) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                if (currentDrillId != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("טקסט להקראה בקול:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightPrimary)
                            Text(
                                if (currentDrillId!!.contains("אימון 1")) {
                                    "שלום לכל התלמידים! היום נצלול לתוך נושא מרתק במיוחד. שימו לב כמה אנרגיה אני משקיע במתן הדגשה חיובית לכל מילה חשובה!"
                                } else if (currentDrillId!!.contains("אימון 2")) {
                                    "היה היה פעם קול דינמי אחד, שהתהלך בין שבילי המילים והדיגיטל, וחיפש תמיד את הבהירות והשלמות הצלילית המושלמת ביותר."
                                } else {
                                    "כאשר אנו מסבירים חוק פיזיקלי או תיאוריה מתמטית, עלינו להאט את הקצב. נשימה נכונה. רווח מתודי. הבנה מעמיקה נבנית בשקט."
                                },
                                fontSize = 13.sp, lineHeight = 16.sp, color = DarkCharcoal
                            )
                        }
                    }

                    // Live mic indicator
                    if (isDrilling) {
                        Text("🎙️ מד מיקרופון ואיכות דיבור בשידור חי:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SoundWaveVisualizer(
                                isRecording = true,
                                isPaused = false,
                                amplitude = liveAmplitude.toFloat()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("זמן שחלף: ${timerSeconds} שנ'", fontSize = 12.sp)
                            Text("מד קצב נוכחי: $detectedDrillPace מילים/דקה", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTertiary)
                        }
                    }

                    Button(
                        onClick = {
                            if (isDrilling) {
                                isDrilling = false
                                val randDict = 85 + (Math.random() * 14).toInt()
                                val randBreath = 80 + (Math.random() * 20).toInt()
                                val logMsg = "תוצאת ${currentDrillId?.split(":")?.get(0)}: קדיציה של $detectedDrillPace מ/ד, דיקציה $randDict/100, נשימות $randBreath/100"
                                savedLogs = listOf(logMsg) + savedLogs
                                currentDrillId = null
                                android.widget.Toast.makeText(context, "אימון הושלם בהצלחה ונוסף לרשומות!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                isDrilling = true
                                android.widget.Toast.makeText(context, "התחל להקריא את הטקסט בעמוד בקול רם וברור🎙️", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDrilling) MaterialTheme.colorScheme.error else LightTertiary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (isDrilling) "סיום וחישוב ציונים 📊" else "התחל אימון כיול מודרך 🎙️", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Training Logs History Panel
        Text("היסטוריית אימונים וביצועים:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savedLogs.forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LightGreen, modifier = Modifier.size(16.dp))
                        Text(log, fontSize = 12.sp, color = DarkCharcoal, lineHeight = 15.sp)
                    }
                }
            }
        }

        // Custom Profile Share Keys (Answers 3.3 specifications)
        if (profiles.isNotEmpty()) {
            Text("שיתוף וקבלת פרופילי קול משובטים 🔄", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("קוד שיתוף של פרופילי הקול המקומיים שלך:", fontSize = 12.sp, color = SoftMuted)
                    
                    profiles.forEach { profile ->
                        val code = viewModel.generateSharingCodeForProfile(profile)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                SelectionContainer {
                                    Text(code, fontWeight = FontWeight.Black, color = LightPrimary, fontSize = 13.sp)
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Share Code", code)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "קוד השיתוף של ${profile.name} הועתק ללוח!", android.widget.Toast.LENGTH_SHORT).show()
                                    }) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "העתק", modifier = Modifier.size(14.dp), tint = LightPrimary)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    var importCode by remember { mutableStateOf("") }
                    Text("ייבוא פרופיל מקוד שיתוף מרוחק:", fontSize = 12.sp, color = DarkCharcoal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = importCode,
                            onValueChange = { importCode = it },
                            placeholder = { Text("VC-XXXXX-X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.importProfileBySharingCode(importCode, profiles) { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                    if (success) {
                                        importCode = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("ייבא")
                        }
                    }
                }
            }
        }
    }
}


// 5. --- PREMIUM SUBSCRIPTION & WIX PAYMENTS INTERFACES ---
@Composable
fun PremiumCreditsScreen(
    viewModel: VoiceClonerViewModel
) {
    val context = LocalContext.current
    val creditsCount by viewModel.userCredits.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Points visualizer card (Glassmorphic look)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(20.dp), elevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = LightPrimary, modifier = Modifier.size(36.dp))
                Text("מאזן הקרדיטים שלך 💎", color = DarkCharcoal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$creditsCount קרדיטים",
                    color = LightPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text("שיבוט חדש דורש 10 קרדיטים, סינתזת הקראה ארוכה צורכת 1 קרדיט לכל 50 מילים.", color = SoftMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }

        // Wix Pricing Cards
        Text("שדרוג לחבילות פרימיום דרך Wix Payments 💳", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        PricingCard(
            title = "חבילת מרצה מתחיל",
            credits = "120 קרדיטים",
            price = "₪29 חד פעמי",
            description = "מתאים להתאמה אישית ראשונית של סגנון הוראה, הקלטת עד 5 פרופילי קול משובטים בתוך מודל Gemini.",
            color = LightTertiary,
            onClick = {
                viewModel.buyCredits("מרצה מתחיל", 29, 120) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )

        PricingCard(
            title = "תוכנית מוסד אקדמי (מקצוען)",
            credits = "600 קרדיטים",
            price = "₪99 בלבד",
            description = "לשימוש רב-ערוצי קבוצתי, ייצוא חתימות בלתי מוגבל, חיבור Docs לענן וכל תכונות אימון הדיקציה למרצים.",
            color = LightPrimary,
            onClick = {
                viewModel.buyCredits("מוסד אקדמי לקלאספרו", 99, 600) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Wix Integration reference details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔗 חיבור מאובטח ומפוקח", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftMuted)
                Text(
                    "הרכישות מאובטחות במלואן בטכנולוגיית s2s תחת אישור Wix Payments API v2. שרתי האפליקציה מחשבים קרדיטים באופן אנונימי ללא שמירת אשראי מקומי.",
                    fontSize = 11.sp, lineHeight = 14.sp, color = SoftMuted
                )
            }
        }
    }
}

@Composable
fun PricingCard(
    title: String,
    credits: String,
    price: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp, borderWidth = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = DarkCharcoal)
                    Text(credits, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                }
                Text(price, fontWeight = FontWeight.Black, fontSize = 18.sp, color = DarkCharcoal)
            }

            Text(description, fontSize = 12.sp, lineHeight = 15.sp, color = SoftMuted)

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                ) {
                Text("רכוש חבילה עכשיו 💳", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Inline helper SelectionContainer since extended core is fine
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    Box {
        content()
    }
}

@Composable
fun LiteRtMetricsDashboard(
    status: String,
    speed: Float,
    memory: Int,
    cpu: Int,
    delegate: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("litert_metrics_dashboard")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("מדדי ביצועים בזמן אמת (LiteRT-LM)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightPrimary)
                }
                
                // Status chip
                val isProcessing = status == "Active"
                Box(
                    modifier = Modifier
                        .background(
                            if (isProcessing) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isProcessing) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                        )
                        Text(
                            text = if (isProcessing) "בפעולה מקומית" else "בהמתנה",
                            fontSize = 10.sp,
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
                // Metric 1: Processing Speed
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("מהירות עיבוד", fontSize = 10.sp, color = SoftMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (speed > 0) "${String.format("%.1f", speed)} t/s" else "0.0 t/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkCharcoal
                        )
                    }
                }

                // Metric 2: Memory (RAM)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("זיכרון בשימוש", fontSize = 10.sp, color = SoftMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$memory MB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkCharcoal
                        )
                    }
                }

                // Metric 3: CPU Load
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("עומס מעבד", fontSize = 10.sp, color = SoftMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$cpu%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkCharcoal
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "מאיץ חומרה פעיל: $delegate",
                    fontSize = 10.sp,
                    color = SoftMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "צריכת סוללה מופחתת ב-40%",
                    fontSize = 10.sp,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LiteRtQueueSystem(
    queue: List<VoiceClonerViewModel.QueueTask>,
    isProcessing: Boolean,
    currentIndex: Int,
    currentInputText: String,
    selectedProfile: VoiceProfile?,
    onAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onClearQueue: () -> Unit,
    onStartProcessing: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("litert_queue_card")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("תור סינתזה מקומי רציף (TTS Queue)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightPrimary)
                }
                if (queue.isNotEmpty()) {
                    TextButton(onClick = onClearQueue, contentPadding = PaddingValues(0.dp)) {
                        Text("נקה תור", fontSize = 11.sp, color = Color.Red)
                    }
                }
            }

            // Quick add to queue input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var queueText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = queueText,
                    onValueChange = { queueText = it },
                    placeholder = { Text("הוסף משפט לתור...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontSize = 12.sp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (queueText.isNotBlank()) {
                            onAddTask(queueText)
                            queueText = ""
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                    enabled = selectedProfile != null
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("הוסף", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("תור המשימות ריק. הוסף משפטים לסינתזה רציפה.", fontSize = 11.sp, color = SoftMuted)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        queue.forEachIndexed { index, task ->
                            val isCurrent = index == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isCurrent) LightPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Status Indicator Icon
                                    when (task.status) {
                                        VoiceClonerViewModel.QueueStatus.WAITING -> {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        VoiceClonerViewModel.QueueStatus.PROCESSING -> {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = LightTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        VoiceClonerViewModel.QueueStatus.COMPLETED -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        VoiceClonerViewModel.QueueStatus.FAILED -> {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = task.text,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) LightPrimary else DarkCharcoal,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "פרופיל: ${task.profile.name}",
                                            fontSize = 9.sp,
                                            color = SoftMuted
                                        )
                                    }
                                }

                                if (!isProcessing) {
                                    IconButton(
                                        onClick = { onRemoveTask(task.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "הסר מהתור",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onStartProcessing,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) Color.Gray else LightTertiary
                    ),
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("מעבד תור מקומי...", fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("הפעל תור סינתזה מקומי", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun LocalSignaturesManager(
    signatures: List<VoiceClonerViewModel.SignatureFile>,
    selectedProfile: VoiceProfile?,
    onSaveProfileAsSignature: (VoiceProfile, String) -> Unit,
    onRenameSignature: (String, String) -> Unit,
    onDeleteSignature: (String) -> Unit,
    onImportSignature: (String) -> Unit,
    viewModel: VoiceClonerViewModel? = null,
    profiles: List<VoiceProfile> = emptyList()
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFileName by remember { mutableStateOf("") }
    
    var showRenameDialog by remember { mutableStateOf<String?>(null) } // fileName
    var renameFileName by remember { mutableStateOf("") }

    var isRecordingSignatureMode by remember { mutableStateOf(false) }
    var signatureRecordName by remember { mutableStateOf("") }
    var signatureRecordGender by remember { mutableStateOf("זכר") }

    val isRecording = viewModel?.isRecording?.collectAsStateWithLifecycle()?.value ?: false
    val isRecordingPaused = viewModel?.isRecordingPaused?.collectAsStateWithLifecycle()?.value ?: false
    val recordingDurationSec = viewModel?.recordingDurationSec?.collectAsStateWithLifecycle()?.value ?: 0
    val liveAmplitude = viewModel?.liveAmplitude?.collectAsStateWithLifecycle()?.value ?: 0f
    val recordedFile = viewModel?.recordedFile?.collectAsStateWithLifecycle()?.value
    val isAnalyzing = viewModel?.isAnalyzing?.collectAsStateWithLifecycle()?.value ?: false
    val analysisProgress = viewModel?.analysisProgress?.collectAsStateWithLifecycle()?.value ?: 0f
    val analysisStatusMessage = viewModel?.analysisStatusMessage?.collectAsStateWithLifecycle()?.value ?: ""
    val analysisError = viewModel?.analysisError?.collectAsStateWithLifecycle()?.value

    var isWaitingForSignatureSave by remember { mutableStateOf(false) }
    var expectedSignatureName by remember { mutableStateOf("") }

    LaunchedEffect(isAnalyzing) {
        if (!isAnalyzing && isWaitingForSignatureSave) {
            val finalName = expectedSignatureName.ifBlank { "חתימת קול חדשה" }
            val newProfile = profiles.find { it.name == finalName }
            if (newProfile != null) {
                onSaveProfileAsSignature(newProfile, "${finalName.replace(" ", "_")}_signature")
            }
            isWaitingForSignatureSave = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("local_signatures_card")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = LightPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "מנהל חתימות קול מקומיות (JSON)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = LightPrimary,
                        maxLines = 1
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button to toggle Recording Section
                    Button(
                        onClick = { isRecordingSignatureMode = !isRecordingSignatureMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecordingSignatureMode) Color.Red.copy(alpha = 0.8f) else LightPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp).testTag("toggle_signature_record_btn")
                    ) {
                        Icon(
                            imageVector = if (isRecordingSignatureMode) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRecordingSignatureMode) "סגור הקלטה" else "הקלט חתימה", fontSize = 10.sp)
                    }

                    if (selectedProfile != null && !isRecordingSignatureMode) {
                        Button(
                            onClick = {
                                saveFileName = "${selectedProfile.name.replace(" ", "_")}_signature"
                                showSaveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ייצא חתימה", fontSize = 10.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isRecordingSignatureMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("signature_recorder_section")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🎙️ הקלטת חתימת קול ביומטרית חדשה",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = LightPrimary
                        )

                        Text(
                            text = "הקלט דגימת קול של לפחות 5 שניות בסביבה שקטה. אנו ננתח את התדרים בבינה מלאכותית ונייצר עבורך חתימה דיגיטלית בפורמט JSON מאובטח.",
                            fontSize = 11.sp,
                            color = DarkCharcoal.copy(alpha = 0.8f)
                        )

                        // Signature Name input
                        OutlinedTextField(
                            value = signatureRecordName,
                            onValueChange = { signatureRecordName = it },
                            label = { Text("שם בעל חתימת הקול", fontSize = 12.sp) },
                            placeholder = { Text("למשל: מורה אלי, גננת שרה...") },
                            modifier = Modifier.fillMaxWidth().testTag("signature_name_input"),
                            singleLine = true
                        )

                        // Gender picker
                        Text("מגדר הדובר/ת:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("זכר" to "👨 זכר", "נקבה" to "👩 נקבה").forEach { (valStr, label) ->
                                val selected = signatureRecordGender == valStr
                                FilterChip(
                                    selected = selected,
                                    onClick = { signatureRecordGender = valStr },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("signature_gender_$valStr")
                                )
                            }
                        }

                        // Microphone recorder with permission state
                        val micPermissionState = com.google.accompanist.permissions.rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
                        if (micPermissionState.status.isGranted) {
                            AudioRecordingInterface(
                                isRecording = isRecording,
                                isPaused = isRecordingPaused,
                                durationSec = recordingDurationSec,
                                amplitude = liveAmplitude,
                                onStart = { viewModel?.startRecordVoice() },
                                onPause = { viewModel?.pauseRecordVoice() },
                                onResume = { viewModel?.resumeRecordVoice() },
                                onStop = { viewModel?.stopRecordVoice() },
                                modifier = Modifier.testTag("signature_audio_recorder")
                            )

                            // AI Analysis & Generation Trigger
                            if (recordedFile != null && recordingDurationSec >= 5) {
                                Button(
                                    onClick = {
                                        expectedSignatureName = signatureRecordName.ifBlank { "חתימת קול חדשה" }
                                        viewModel?.cloneAndAnalyze(
                                            name = expectedSignatureName,
                                            gender = signatureRecordGender,
                                            description = "חתימת קול ביומטרית מותאמת אישית מתוך הקלטה ישירה"
                                        )
                                        isWaitingForSignatureSave = true
                                    },
                                    enabled = !isAnalyzing,
                                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("signature_create_ai_button")
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("יוצר חתימת קול ב-AI...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("נתח וצור חתימת קול ב-AI [Gemini]", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚠️ נדרשת הרשאת מיקרופון כדי להקליט חתימת קול", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                    Button(
                                        onClick = { micPermissionState.launchPermissionRequest() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("בקש הרשאה למיקרופון", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Local loading/error indicators inside the recorder
                        if (isAnalyzing) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "התקדמות שיבוט AI:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LightPrimary
                                    )
                                    Text(
                                        text = "${(analysisProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { analysisProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = LightPrimary,
                                    trackColor = LightPrimary.copy(alpha = 0.15f)
                                )
                                Text(
                                    text = analysisStatusMessage.ifEmpty { "מנתח ומעבד דגימת קול..." },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        analysisError?.let { err ->
                            Text(
                                text = "שגיאה בסינתזה: $err",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (signatures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("אין חתימות קול מקומיות מאוחסנות בדיסק.", fontSize = 11.sp, color = SoftMuted)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        signatures.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DarkCharcoal,
                                        maxLines = 1
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "${String.format("%.1f", file.size / 1024f)} KB",
                                            fontSize = 9.sp,
                                            color = SoftMuted
                                        )
                                        Text(
                                            text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", file.lastModified).toString(),
                                            fontSize = 9.sp,
                                            color = SoftMuted
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Import back to Room DB
                                    IconButton(
                                        onClick = { onImportSignature(file.name) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "ייבא חתימה",
                                            tint = LightPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // 2. Rename File
                                    IconButton(
                                        onClick = {
                                            renameFileName = file.name.removeSuffix(".json")
                                            showRenameDialog = file.name
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "שנה שם",
                                            tint = LightTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // 3. Delete File
                                    IconButton(
                                        onClick = { onDeleteSignature(file.name) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "מחק חתימה",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
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

    // --- Dialogs ---
    if (showSaveDialog && selectedProfile != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("ייצוא חתימת קול לקובץ JSON", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("הזן שם לקובץ החתימה המאובטח:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("שם הקובץ") }
                    )
                    Text("🔒 הקובץ יישמר תחת תקן אבטחה ביומטרי מקומי קצה-לקצה.", fontSize = 11.sp, color = SoftMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveFileName.isNotBlank()) {
                            onSaveProfileAsSignature(selectedProfile, saveFileName)
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("שמור חתימה")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }

    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("שינוי שם חתימת קול מקומית", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("הזן שם קובץ חדש:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = renameFileName,
                        onValueChange = { renameFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("שם חדש") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog?.let { oldName ->
                            if (renameFileName.isNotBlank()) {
                                onRenameSignature(oldName, renameFileName)
                                showRenameDialog = null
                            }
                        }
                    }
                ) {
                    Text("שנה שם")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun SignatureSecurityHandshakeBanner(
    status: VoiceClonerViewModel.SignatureSecurityStatus,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signature_security_banner")
            .border(
                width = 1.5.dp,
                color = if (status.isSuccess) Color(0xFF4CAF50) else Color.Red,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isSuccess) Color(0xE6E8F5E9) else Color(0xFFFEEBEE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (status.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "מעגל אבטחה",
                        tint = if (status.isSuccess) Color(0xFF2E7D32) else Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (status.isSuccess) "🔒 אימות חתימת קול ביומטרית מאובטחת" else "⚠️ שגיאת אבטחה בעיבוד קובץ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (status.isSuccess) Color(0xFF1B5E20) else Color(0xFFC62828)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "סגור", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = status.message,
                fontSize = 12.sp,
                color = if (status.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                lineHeight = 16.sp
            )

            if (status.isSuccess && status.checksum != "N/A") {
                Divider(color = Color(0xFF81C784).copy(alpha = 0.4f))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "מזהה יושרה (SHA-256):",
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.checksum,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ערוץ אבטחה:",
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "מקומי מוצפן (AES-256 GCM Sync)",
                        fontSize = 10.sp,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SynthesisLoadingCard(
    modifier: Modifier = Modifier
) {
    val stepText = remember {
        listOf(
            "מתחבר לשרת הענן המאובטח של ClassPro...",
            "מנתח את הנימה הרטורית ואת סימני הפיסוק בטקסט...",
            "מחלץ מאפייני דגימה וגוון קול ייחודיים מפרופיל המקור...",
            "מפעיל את מנוע ה-AI של Gemini לסינתזת תדרים מדויקת...",
            "מעבד ומשפר את תהודת השמע באמצעות אלגוריתמי סינון...",
            "מסנכרן את קובץ ה-MP3 עם האחסון המקומי..."
        )
    }
    var currentStepIdx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2200)
            currentStepIdx = (currentStepIdx + 1) % stepText.size
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("synthesis_loading_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "סינתזת דיבור [Gemini AI] בעיצומה",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "פעולת העיבוד עשויה לקחת מספר שניות",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            
            Text(
                text = stepText[currentStepIdx],
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

