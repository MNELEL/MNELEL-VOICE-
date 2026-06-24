package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.glassmorphic
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag
import com.example.VoiceClonerViewModel
import com.example.data.VoiceProfile
import com.example.data.DiarizationSegment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSpeakerDiarizationView(
    viewModel: VoiceClonerViewModel,
    profiles: List<VoiceProfile>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diarizationSegments by viewModel.diarizationSegments.collectAsStateWithLifecycle()
    val isDiarizing by viewModel.isDiarizing.collectAsStateWithLifecycle()
    val diarizationError by viewModel.diarizationError.collectAsStateWithLifecycle()
    val diarizationFile by viewModel.diarizationAudioFile.collectAsStateWithLifecycle()

    var showHelpDialog by remember { mutableStateOf(false) }

    // File Picker for Multi-Speaker Audio Recording
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = copyUriToTempFile(context, it)
            if (file != null) {
                viewModel.setDiarizationAudioFile(file)
            } else {
                android.widget.Toast.makeText(context, "שגיאה בקריאת הקובץ הנבחר", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(20.dp), elevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
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
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "זיהוי דוברים אוטומטי (Diarization) 👥",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "הפרדת שיחה לפי פרופילי קול משובטים",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "הסבר על התכונה",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (diarizationFile == null && diarizationSegments.isEmpty()) {
                // Empty state: Select File to get started
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "בחר קובץ שיחה או הקלטה המכילה מספר אנשים מדברים. הבינה המלאכותית תזהה מי מדבר בכל רגע על בסיס פרופילי הקול ששמרת.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("בחרו קובץ הקלטה מהמכשיר 🎵", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // File Selected or Analyzed
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // File summary line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null
                                )
                                Column {
                                    Text(
                                        text = diarizationFile?.name ?: "קובץ הקלטה שנבחר",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${(diarizationFile?.length() ?: 0) / 1024} KB",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.clearDiarizationResult() },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "הסר קובץ"
                                )
                            }
                        }
                    }

                    // Error text if exists
                    diarizationError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Loader
                    if (isDiarizing) {
                        val steps = remember {
                            listOf(
                                "קורא את קובץ השמע המיובא...",
                                "מבצע ספקטרוגרפיה ומזהה מעברי דוברים...",
                                "משווה חתימות קול מול פרופילים קיימים בבינה מלאכותית...",
                                "מקטלג מקטעי שיח ומבצע סינון רעשי סביבה...",
                                "מעבד ומחלץ תמליל ותמלול שיחה באמצעות Gemini...",
                                "בונה מפת שיח רב-ערוצית..."
                            )
                        }
                        var currentStep by remember { mutableStateOf(0) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(2000)
                                currentStep = (currentStep + 1) % steps.size
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("diarization_loading_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Column {
                                        Text(
                                            text = "זיהוי דוברים וניתוח שיח [AI] בעיצומו",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "מנתח שיח רב-משתתפים בעזרת מודל Gemini",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                
                                Text(
                                    text = steps[currentStep],
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (diarizationSegments.isEmpty()) {
                        // File loaded but not analyzed yet
                        Button(
                            onClick = { viewModel.runSpeakerDiarization(profiles) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("הפעל אנליזה וזיהוי דוברים אוטומטי 🚀", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Result Segments Timeline Display
                    if (diarizationSegments.isNotEmpty()) {
                        Text(
                            text = "תוצאות האנליזה והפרדת הדוברים:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Segment Cards list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            diarizationSegments.forEach { segment ->
                                DiarizedSegmentRow(
                                    segment = segment,
                                    availableProfiles = profiles,
                                    onOverrideAssignment = { selectedProfile ->
                                        viewModel.updateSegmentAssignment(segment.id, selectedProfile)
                                    }
                                )
                            }
                        }

                        // Export options toolbar
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            val isDriveSyncing by viewModel.driveSyncing.collectAsStateWithLifecycle()
                            
                            Button(
                                onClick = {
                                    val transcript = diarizationSegments.joinToString("\n\n") { seg ->
                                        val speaker = seg.assignedProfileName ?: seg.detectedSpeakerName
                                        "[${seg.startTime} - ${seg.endTime}] $speaker:\n${seg.text}"
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Meeting Transcript", transcript)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "סיכום ותמלול הדוברים הועתק ללוח! 📋", android.widget.Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("העתק את תמלול השיחה מחולק לפי דוברים", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val transcript = diarizationSegments.joinToString("\n\n") { seg ->
                                        val speaker = seg.assignedProfileName ?: seg.detectedSpeakerName
                                        "[${seg.startTime} - ${seg.endTime}] $speaker:\n${seg.text}"
                                    }
                                    viewModel.saveTranscriptToGoogleDoc(transcript, "תמלול שיחה מפוצלת") { success, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = !isDriveSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isDriveSyncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("שמור תמלול פדגוגי כ-Google Doc [Drive] 📄", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "איך עובד זיהוי דוברים (Diarization)? 🤔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. העלאת קובץ קולי: בצעו בחירה של שיחה מוקלטת (כגון פגישה, ראיון או דיון).",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "2. בדיקת טביעת קול: מודל ה-Gemini מנתח את התכונות האקוסטיות (תדר, קצב, טון) בהקלטה ומשווה אותן לפרופילי הקול ששמרתם באפליקציה.",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "3. חלוקה למקטעים: השיחה מחולקת באופן כרונולוגי עם שיוך אוטומטי לכל דובר וציון רמת אמינות הזיהוי.",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "4. עריכה ידנית: אם זיהוי מסוים אינו מדויק, תוכלו ללחוץ על הדובר ולבחור בצורה ידנית קול קיים לשיוך מתוקן.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("הבנתי, תודה", fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
fun DiarizedSegmentRow(
    segment: DiarizationSegment,
    availableProfiles: List<VoiceProfile>,
    onOverrideAssignment: (VoiceProfile?) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val matchedProfile = availableProfiles.find { it.id == segment.assignedProfileId }
    val displaySpeakerName = matchedProfile?.name ?: segment.assignedProfileName ?: segment.detectedSpeakerName
    val isUnknown = matchedProfile == null

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Timestamps & Speaker Assignment Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${segment.startTime} - ${segment.endTime}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interactive Dropdown Trigger for Override
                Box {
                    Surface(
                        onClick = { dropdownExpanded = true },
                        color = if (isUnknown) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnknown) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isUnknown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isUnknown) "$displaySpeakerName ⚙️" else "$displaySpeakerName (משויך) ⚙️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnknown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("סמן כדובר לא ידוע 👥", fontWeight = FontWeight.Medium) },
                            onClick = {
                                onOverrideAssignment(null)
                                dropdownExpanded = false
                            }
                        )
                        Divider()
                        availableProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    onOverrideAssignment(profile)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Transcript text
            Text(
                text = "\"${segment.text}\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Characteristics and confidence bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (segment.voiceCharacteristics.isNotEmpty()) {
                    Text(
                        text = "תכונות שמע: ${segment.voiceCharacteristics}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Small confidence percentage pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (segment.confidence >= 80) Color(0xFFE6F4EA)
                            else if (segment.confidence >= 50) Color(0xFFFEF7E0)
                            else Color(0xFFFCE8E6)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "דיוק: ${segment.confidence}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (segment.confidence >= 80) Color(0xFF137333)
                        else if (segment.confidence >= 50) Color(0xFFB06000)
                        else Color(0xFFC5221F)
                    )
                }
            }
        }
    }
}

// Helper to copy selected file URI into local storage, making it readable for base64 conversions
private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val extension = "aac" // Defaulting to AAC
            val tempFile = File(context.cacheDir, "multi_speaker_${UUID.randomUUID()}.$extension")
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            tempFile
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
