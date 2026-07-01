package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VoiceClonerViewModel
import com.example.data.VoiceProfile
import com.example.data.VoiceGenerationResult
import java.io.File

private val BrandNavy = Color(0xFF1B2A4A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastStudioScreen(
    viewModel: VoiceClonerViewModel,
    profiles: List<VoiceProfile>
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("podcast_studio_prefs", android.content.Context.MODE_PRIVATE) }
    var textInput by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf<VoiceProfile?>(null) }
    var lastSavedTime by remember { mutableStateOf<String?>(null) }
    
    val isSynthesizingLongText by viewModel.isSynthesizingLongText.collectAsStateWithLifecycle()
    val longTextProgress by viewModel.longTextProgress.collectAsStateWithLifecycle()
    val longTextStatus by viewModel.longTextStatus.collectAsStateWithLifecycle()
    val synthesizeError by viewModel.synthesizeError.collectAsStateWithLifecycle()
    
    val recentGenerations by viewModel.recentGenerations.collectAsStateWithLifecycle()
    val isPlayingResultId by viewModel.isPlayingResultId.collectAsStateWithLifecycle()

    // Load saved draft on initial launch
    LaunchedEffect(Unit) {
        val savedDraft = sharedPrefs.getString("text_draft", "") ?: ""
        if (savedDraft.isNotEmpty()) {
            textInput = savedDraft
            val savedTime = sharedPrefs.getString("text_draft_time", "") ?: ""
            if (savedTime.isNotEmpty()) {
                lastSavedTime = savedTime
            }
        }
    }

    // Auto-save logic: Periodic save every 30 seconds
    val currentTextForPeriodic by rememberUpdatedState(textInput)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            if (currentTextForPeriodic.isNotEmpty()) {
                sharedPrefs.edit()
                    .putString("text_draft", currentTextForPeriodic)
                    .apply()
                val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                val formattedTime = sdf.format(java.util.Date())
                sharedPrefs.edit().putString("text_draft_time", formattedTime).apply()
                lastSavedTime = formattedTime
            }
        }
    }

    // Auto-save logic: Debounced save on typing pause
    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            kotlinx.coroutines.delay(1500L) // Wait 1.5 seconds after typing stops
            sharedPrefs.edit()
                .putString("text_draft", textInput)
                .apply()
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            val formattedTime = sdf.format(java.util.Date())
            sharedPrefs.edit().putString("text_draft_time", formattedTime).apply()
            lastSavedTime = formattedTime
        }
    }

    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty() && selectedProfile == null) {
            selectedProfile = profiles.first()
        }
    }

    val demoText = """
        שלום לכולם וברוכים הבאים לפרק מיוחד של פודקאסט המדע והטכנולוגיה של קלאס-פרו. היום נדבר על אחד הנושאים המרתקים ביותר של המאה העשרים ואחת: הבינה המלאכותית היוצרת והשפעתה על עולם החינוך וההוראה.
        בעשור האחרון חלה פריצת דרך דרמטית ביכולות של מודלים ממוחשבים לעבד שפה, להבין הקשרים ולייצר תוכן שנשמע אנושי לחלוטין. מורים ואנשי חינוך בכל רחבי העולם מתחילים לשלב את הכלים הללו כדי להתאים את חומרי הלמידה באופן אישי לכל תלמיד ותלמידה.
        بמקום מערך שיעור אחד שמתאים לכולם, המערכת יכולה להסביר מושגים מורכבים בפיזיקה, מתמטיקה או ספרות במגוון דרכים שונות, לפי העדפותיו האישיות של הלומד. הטכנולוגיה מאפשרת לנו לערוך סימולציות אינטראקטיביות, לתרגם טקסטים בזמן אמת, ואפילו לספק משוב אישי ומיידי המבוסס על אבחון מדויק של קול והגייה.
        יחד עם זאת, עולות שאלות חשובות של אתיקה, מקוריות, ושמירה על הקשר האנושי החם שהוא לב לבה של העשייה החינוכית. איך אנו מוודאים שהטכנולוגיה משמשת ככלי עזר ולא כמחליפה לקשר הבינאישי? על כך ועוד נרחיב בפרק של היום.
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Studio Icon",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "סטודיו פודקאסט והרצאות 🎙️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ייצור הרצאות ארוכות ופודקאסטים איכותיים. האפליקציה מחלקת את הטקסט לחלקים (chunking) וממזגת אותם לקובץ אחד באופן אוטומטי.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Speaker Selection
        item {
            Column {
                Text(
                    text = "בחר פרופיל קול לקריינות:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (profiles.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "לא נמצאו פרופילי קול משובטים. אנא הקלט קול במסך הבית או ייבא חתימה קולית בגלריה לפני השימוש בסטודיו.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profiles.forEach { profile ->
                            val isSelected = selectedProfile?.id == profile.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedProfile = profile }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = profile.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${profile.frequencyHz} Hz | ${profile.gender}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Text input section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "טקסט ההרצאה / פודקאסט:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lastSavedTime?.let { time ->
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "טיוטה נשמרה $time",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (textInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    textInput = ""
                                    sharedPrefs.edit().remove("text_draft").remove("text_draft_time").apply()
                                    lastSavedTime = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "נקה טקסט",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = { textInput = demoText },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("טען טקסט לדוגמה", fontSize = 12.sp)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("הקלד או הדבק כאן את הטקסט הארוך לייצור (ספרים, הרצאות, פודקאסטים)...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Text stats
                val charCount = textInput.length
                val estimatedMinutes = if (charCount > 0) Math.ceil(charCount / 850.0).toInt() else 0
                val estimatedChunks = if (charCount > 0) Math.ceil(charCount / 4000.0).toInt() else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("מספר תווים", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$charCount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("זמן משוער", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~$estimatedMinutes דק'", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("חלקי שמע (Chunks)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$estimatedChunks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Error message if any
        synthesizeError?.let { err ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "שגיאה: $err",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Synthesis progress
        if (isSynthesizingLongText) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = longTextStatus,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${(longTextProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { longTextProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Action Button
        item {
            Button(
                onClick = {
                    val prof = selectedProfile
                    if (prof != null) {
                        viewModel.synthesizeLongText(textInput, prof)
                    }
                },
                enabled = !isSynthesizingLongText && textInput.isNotBlank() && selectedProfile != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                if (isSynthesizingLongText) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ייצר הרצאה / פודקאסט ✨", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Generated Podcasts Section
        item {
            Text(
                text = "הרצאות ופודקאסטים שיוצרו בסטודיו:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val studioGenerations = recentGenerations.filter { it.audioPath.contains("podcast_") }

        if (studioGenerations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "אין פודקאסטים מיוצרים עדיין. הזן טקסט למעלה ולחץ על כפתור הייצור.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(studioGenerations) { generation ->
                val isPlaying = isPlayingResultId == generation.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.stopResultSample()
                                } else {
                                    viewModel.playResultSample(generation)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "קריינות מאת: ${generation.profileName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = generation.inputText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.exportResultToMp3(generation, context) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteResult(generation) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
