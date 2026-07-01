package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VoiceClonerViewModel
import com.example.data.VoiceGenerationResult
import com.example.data.VoiceProfile
import com.example.ui.theme.*

// ─── Color tokens (local to this screen) ────────────────────────────
private val GradientHeader = Brush.linearGradient(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
)
private val GradientCta = Brush.linearGradient(
    listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
)
private val CardBg = Color(0xFFF4F6FB)
private val SuccessGreen = Color(0xFF10B981)
private val WarnOrange = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)

// ─── Utility ────────────────────────────────────────────────────────
private fun statsColor(chunks: Int): Color = when {
    chunks <= 3 -> SuccessGreen
    chunks <= 10 -> WarnOrange
    else -> Color(0xFF6366F1)
}

private fun estimatedMinutes(chars: Int) = if (chars > 0) maxOf(1, chars / 850) else 0
private fun estimatedChunks(chars: Int) = if (chars > 0) maxOf(1, (chars + 3999) / 4000) else 0

// ─── Sub-composables ────────────────────────────────────────────────
@Composable
private fun ScreenHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GradientHeader)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic, 
                    null,
                    tint = Color.White, 
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    "סטודיו הרצאות ופודקאסטים",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    "הזן טקסט ארוך — האפליקציה מחלקת ומייצרת בקולך",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    profiles: List<VoiceProfile>,
    selected: VoiceProfile?,
    onSelect: (VoiceProfile) -> Unit
) {
    if (profiles.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3CD))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = WarnOrange, modifier = Modifier.size(20.dp))
            Text(
                "עבור ללשונית «הקלטה» כדי ליצור פרופיל קול ראשון",
                fontSize = 13.sp,
                color = Color(0xFF92400E),
                lineHeight = 17.sp
            )
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected?.let { "🎙️  ${it.name} · ${it.frequencyHz} Hz · צלילות ${it.clarityScore}%" } ?: "בחר פרופיל קול...",
            onValueChange = {},
            label = { Text("פרופיל קול") },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = LightPrimary
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LightPrimary,
                unfocusedBorderColor = LightBorder,
                focusedLabelColor = LightPrimary
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LightSurface)
        ) {
            profiles.forEach { p ->
                val isActive = selected?.id == p.id
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isActive) LightPrimary else LightBg,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person, 
                                    null,
                                    tint = if (isActive) Color.White else SoftMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${p.frequencyHz} Hz · צלילות ${p.clarityScore}% · ${p.gender}",
                                    fontSize = 11.sp, 
                                    color = SoftMuted
                                )
                            }
                            if (isActive) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = LightPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    onClick = {
                        onSelect(p)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isActive) LightPrimary.copy(alpha = 0.04f) else Color.Transparent
                    )
                )
                if (profiles.indexOf(p) < profiles.lastIndex) {
                    HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TextStatsRow(chars: Int) {
    val chunks = estimatedChunks(chars)
    val mins = estimatedMinutes(chars)
    val accent by animateColorAsState(
        targetValue = statsColor(chunks),
        animationSpec = tween(500),
        label = "statsColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatPill(label = "תווים", value = if (chars > 0) "%,d".format(chars) else "—", tint = accent)
        VerticalDivider(modifier = Modifier.height(28.dp), color = accent.copy(alpha = 0.2f))
        StatPill(label = "דקות", value = if (mins > 0) "~$mins" else "—", tint = accent)
        VerticalDivider(modifier = Modifier.height(28.dp), color = accent.copy(alpha = 0.2f))
        StatPill(label = "חלקים", value = if (chunks > 0) "$chunks" else "—", tint = accent)
    }
}

@Composable
private fun StatPill(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = tint)
        Text(label, fontSize = 10.sp, color = SoftMuted, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun SynthesisProgress(
    status: String,
    progress: Float,
    onCancel: () -> Unit
) {
    val animProg by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightPrimary.copy(alpha = 0.06f))
            .border(1.dp, LightPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LightPrimary
                    )
                    Text(status, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DarkCharcoal)
                }
                Text(
                    "${(animProg * 100).toInt()}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = LightPrimary
                )
            }
            LinearProgressIndicator(
                progress = { animProg },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = LightPrimary,
                trackColor = LightPrimary.copy(alpha = 0.12f)
            )
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("בטל ייצור", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GenerationItem(
    result: VoiceGenerationResult,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play/Stop button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) LightPrimary else LightPrimary.copy(alpha = 0.1f))
                    .clickable { if (isPlaying) onStop() else onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                    null,
                    tint = if (isPlaying) Color.White else LightPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.profileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkCharcoal,
                    maxLines = 1
                )
                Text(
                    result.inputText.take(60) + if (result.inputText.length > 60) "…" else "",
                    fontSize = 12.sp,
                    color = SoftMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, "שתף", tint = LightTertiary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "מחק", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
        // Playing indicator bar
        if (isPlaying) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = LightPrimary,
                trackColor = LightPrimary.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun EmptyGenerationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(LightPrimary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, null, tint = LightPrimary.copy(alpha = 0.5f), modifier = Modifier.size(30.dp))
        }
        Text("עוד אין הרצאות", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = DarkCharcoal)
        Text(
            "הזן טקסט למעלה ולחץ «ייצר אודיו» — הפודקאסט יופיע כאן",
            fontSize = 13.sp,
            color = SoftMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = LightPrimary, modifier = Modifier.size(17.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkCharcoal,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
        HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
        content()
    }
}

@Composable
fun PodcastStudioScreen(
    viewModel: VoiceClonerViewModel,
    profiles: List<VoiceProfile>
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("podcast_studio_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Auto-save & initial load
    var textInput by remember { mutableStateOf(prefs.getString("text_draft", "") ?: "") }
    var selectedProfile by remember { mutableStateOf<VoiceProfile?>(null) }
    var lastSaved by remember { mutableStateOf(prefs.getString("text_draft_time", null)) }

    val isSynthesizing by viewModel.isSynthesizingLongText.collectAsStateWithLifecycle()
    val progress by viewModel.longTextProgress.collectAsStateWithLifecycle()
    val statusMsg by viewModel.longTextStatus.collectAsStateWithLifecycle()
    val error by viewModel.synthesizeError.collectAsStateWithLifecycle()
    val generations by viewModel.recentGenerations.collectAsStateWithLifecycle()
    val playingId by viewModel.isPlayingResultId.collectAsStateWithLifecycle()

    val demoTextHe = """
        שלום לכולם וברוכים הבאים לפרק מיוחד של פודקאסט המדע והטכנולוגיה. היום נדבר על אחד הנושאים המרתקים ביותר של המאה העשרים ואחת: הבינה המלאכותית היוצרת והשפעתה על עולם החינוך וההוראה.
        בעשור האחרון חלה פריצת דרך דרמטית ביכולות של מודלים ממוחשבים לעבד שפה, להבין הקשרים ולייצר תוכן שנשמע אנושי לחלוטין. מורים ואנשי חינוך בכל רחבי העולם מתחילים לשלב את הכלים הללו כדי להתאים את חומרי הלמידה באופן אישי לכל תלמיד ותלמידה.
        במקום מערך שיעור אחד שמתאים לכולם, המערכת יכולה להסביר מושגים מורכבים בפיזיקה, מתמטיקה או ספרות במגוון דרכים שונות, לפי העדפותיו האישיות של הלומד. הטכנולוגיה מאפשרת לנו לערוך סימולציות אינטראקטיביות, לתרגם טקסטים בזמן אמת, ואפילו לספק משוב אישי ומיידי המבוסס על אבחון מדויק של קול והגייה.
        יחד עם זאת, עולות שאלות חשובות של אתיקה, מקוריות, ושמירה על הקשר האנושי החם שהוא לב לבה של העשייה החינוכית. איך אנו מוודאים שהטכנולוגיה משמשת ככלי עזר ולא כמחליפה לקשר הבינאישי? על כך ועוד נרחיב בפרק של היום.
    """.trimIndent()

    // Initialize textInput with Hebrew demo if empty
    LaunchedEffect(Unit) {
        if (textInput.isBlank()) {
            textInput = demoTextHe
        }
    }

    // Auto-select first profile
    LaunchedEffect(profiles) {
        if (selectedProfile == null && profiles.isNotEmpty()) {
            selectedProfile = profiles.first()
        }
    }

    // Auto-save: debounced on typing
    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            kotlinx.coroutines.delay(1500L)
            val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            prefs.edit()
                .putString("text_draft", textInput)
                .putString("text_draft_time", now)
                .apply()
            lastSaved = now
        }
    }

    // TXT file picker
    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r ->
                    textInput = r.readText()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "שגיאה בקריאת הקובץ", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val podcastGenerations = remember(generations) {
        generations.filter { it.audioPath.contains("podcast_") }.sortedByDescending { it.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item { ScreenHeader() }

        // Profile
        item {
            SectionCard(icon = Icons.Default.Person, title = "פרופיל קול") {
                ProfileDropdown(
                    profiles = profiles,
                    selected = selectedProfile,
                    onSelect = { selectedProfile = it }
                )
            }
        }

        // Text input
        item {
            SectionCard(
                icon = Icons.Default.Edit,
                title = "טקסט ההרצאה",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Auto save badge
                        lastSaved?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                Text(it, fontSize = 10.sp, color = SuccessGreen)
                            }
                        }
                        // Load TXT
                        OutlinedButton(
                            onClick = { txtLauncher.launch("text/plain") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("TXT", fontSize = 11.sp, color = LightPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        // Clear
                        if (textInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    textInput = ""
                                    prefs.edit().remove("text_draft").remove("text_draft_time").apply()
                                    lastSaved = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            ) {
                // Stats row (live)
                if (textInput.isNotEmpty()) {
                    TextStatsRow(textInput.length)
                }
                
                // Textarea
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "הדבק כאן את תוכן ההרצאה, הפרק, הדרשה, או הפודקאסט...",
                            color = SoftMuted.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightPrimary,
                        unfocusedBorderColor = LightBorder,
                        focusedContainerColor = LightBg,
                        unfocusedContainerColor = LightBg
                    ),
                    maxLines = 20,
                    textStyle = LocalTextStyle.current.copy(lineHeight = 22.sp, fontSize = 14.sp)
                )
            }
        }

        // Error
        error?.let { err ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerRed.copy(alpha = 0.07f))
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Text(err, fontSize = 13.sp, color = DangerRed, lineHeight = 17.sp)
                }
            }
        }

        // Progress
        if (isSynthesizing) {
            item {
                SynthesisProgress(
                    status = statusMsg,
                    progress = progress,
                    onCancel = { viewModel.cancelLongSynth() }
                )
            }
        }

        // CTA button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!isSynthesizing && textInput.isNotBlank() && selectedProfile != null) GradientCta 
                        else Brush.linearGradient(listOf(SoftMuted.copy(alpha = 0.3f), SoftMuted.copy(alpha = 0.3f)))
                    )
                    .clickable(enabled = !isSynthesizing && textInput.isNotBlank() && selectedProfile != null) {
                        selectedProfile?.let { viewModel.synthesizeLongText(textInput, it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSynthesizing) Icons.Default.HourglassEmpty else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        if (isSynthesizing) "...מייצר אודיו" else "ייצר אודיו",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }

        // Generated results section
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("הרצאות שיוצרו", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkCharcoal)
                if (podcastGenerations.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LightPrimary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${podcastGenerations.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (podcastGenerations.isEmpty()) {
            item { EmptyGenerationsState() }
        } else {
            items(podcastGenerations, key = { it.id }) { gen ->
                GenerationItem(
                    result = gen,
                    isPlaying = playingId == gen.id,
                    onPlay = { viewModel.playResultSample(gen) },
                    onStop = { viewModel.stopResultSample() },
                    onShare = { viewModel.exportResultToMp3(gen, context) },
                    onDelete = { viewModel.deleteResult(gen) }
                )
            }
        }

        // Bottom padding for nav bar clearance
        item { Spacer(Modifier.height(72.dp)) }
    }
}
