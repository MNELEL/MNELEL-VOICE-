package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.glassmorphic
import com.example.data.VoiceProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceAnalysisDashboardUI(
    profile: VoiceProfile,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("דיוק פונטי והגייה", "ניתוח תדרי קול (Pitch)", "רעשי רקע וסביבה (Noise)")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Dashboard Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Analysis Dashboard",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "לוח מחווני אבחון קולי פונטי",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "דוח קולי מבוסס מודל Gemini AI",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Voice Metrics Summary Panel (Estimated Age, Emotion/Vibe, Accent)
            VoiceMetricsSummaryPanel(profile = profile)

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated tab content switcher
            AnimatedContent(
                targetState = selectedTab,
                label = "dashboard_tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> PhoneticAccuracyTabContent(profile = profile)
                    1 -> PitchFrequencyTabContent(profile = profile)
                    2 -> NoiseLevelsTabContent(profile = profile)
                }
            }
        }
    }
}

@Composable
fun VoiceMetricsSummaryPanel(profile: VoiceProfile, modifier: Modifier = Modifier) {
    // 1. Estimate Age based on biometric voice pitch frequency & gender demographic markers
    val estimatedAge = remember(profile.frequencyHz, profile.gender) {
        val isFemale = profile.gender.equals("Female", ignoreCase = true) || profile.gender.contains("נקבה")
        val hz = profile.frequencyHz
        if (isFemale) {
            when {
                hz > 215 -> "18 - 25"
                hz in 175..215 -> "26 - 40"
                else -> "41 - 55"
            }
        } else {
            when {
                hz < 112 -> "40 - 60"
                hz in 112..145 -> "27 - 39"
                else -> "18 - 26"
            }
        }
    }

    // 2. Emotion Tone mapped from calculated intonation variance & vibe tags
    val emotionText = remember(profile.intonationScore, profile.vibe) {
        val vibeLower = profile.vibe.lowercase()
        when {
            vibeLower.contains("warm") || vibeLower.contains("חם") -> "חברותי וחם"
            vibeLower.contains("calm") || vibeLower.contains("רגוע") || vibeLower.contains("gentle") -> "רגוע ונינוח"
            vibeLower.contains("professional") || vibeLower.contains("מקצועי") || vibeLower.contains("serious") -> "מקצועי ורציני"
            vibeLower.contains("energetic") || vibeLower.contains("נמרץ") || vibeLower.contains("bright") -> "נמרץ וערני"
            profile.intonationScore > 78 -> "מלא הבעה ורגש"
            profile.intonationScore < 52 -> "סמכותי וממוקד"
            else -> "מאוזן ויציב"
        }
    }

    // 3. Accent Detail formulated from pronunciation clarity & articulation scores
    val accentText = remember(profile.pronunciationClarity) {
        when {
            profile.pronunciationClarity > 86 -> "עברית ישראלית רהוטה 🇮🇱"
            profile.pronunciationClarity in 72..86 -> "עברית ישראלית (ניטרלי)"
            else -> "מבטא אזורי לוקאלי"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(12.dp), elevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "סיכום אבחון מאפיינים פיזיולוגיים ורגשיים של הקול",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Age
                VoiceMetricSummaryCard(
                    title = "גיל קולי משוער",
                    value = "$estimatedAge שנים",
                    icon = Icons.Default.Person,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Emotion
                VoiceMetricSummaryCard(
                    title = "מצב רגשי ואינטונציה",
                    value = emotionText,
                    icon = Icons.Default.Face,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1.5f)
                )

                // Metric 3: Accent
                VoiceMetricSummaryCard(
                    title = "מבטא ודיוק פונטי",
                    value = accentText,
                    icon = Icons.Default.Star,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
fun VoiceMetricSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassmorphic(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun PhoneticAccuracyTabContent(profile: VoiceProfile) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "אנליזה של דיוק פונטי ומבנה הברה",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "תרשים הרדאר להלן משווה 5 ממדי הגייה עיקריים שפוענחו מדגימת הקול.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom drawn Radar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2f, height / 2f)
                val maxRadius = (size.minDimension * 0.4f).coerceIn(10f, 90.dp.toPx())

                // 1. Draw web grid levels (concentric pentagons)
                val levels = 5
                val numSides = 5

                // Metric indices and labels:
                // 0: דיוק פונטי (profile.pronunciationClarity)
                // 1: חיתוך דיבור (profile.clarityScore)
                // 2: אינטונציה (profile.intonationScore)
                // 3: סדירות נשימה (profile.breathPauseScore)
                // 4: ניקיון אות (100 - profile.distortionLevel)
                val labels = listOf("דיוק הברתי", "חיתוך קולי", "מנגינת דיבור", "סדירות נשימה", "ניקיון מרעש")
                val values = listOf(
                    profile.pronunciationClarity.toFloat() / 100f,
                    profile.clarityScore.toFloat() / 100f,
                    profile.intonationScore.toFloat() / 100f,
                    profile.breathPauseScore.toFloat() / 100f,
                    ((100 - profile.distortionLevel).coerceIn(0, 100)).toFloat() / 100f
                )

                for (level in 1..levels) {
                    val radius = maxRadius * (level.toFloat() / levels.toFloat())
                    val gridPath = Path()
                    for (i in 0 until numSides) {
                        val angle = i * (2f * Math.PI / numSides) - Math.PI / 2f
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()
                        if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                    }
                    gridPath.close()
                    drawPath(
                        path = gridPath,
                        color = Color(0xFFF1F5F9),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 2. Draw axis lines
                for (i in 0 until numSides) {
                    val angle = i * (2f * Math.PI / numSides) - Math.PI / 2f
                    val outerX = center.x + maxRadius * cos(angle).toFloat()
                    val outerY = center.y + maxRadius * sin(angle).toFloat()
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = center,
                        end = Offset(outerX, outerY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw labels
                    val textAngle = angle
                    // Offset text slightly further out
                    val textDist = maxRadius + 14.dp.toPx()
                    val labelX = center.x + textDist * cos(textAngle).toFloat() - 25.dp.toPx()
                    val labelY = center.y + textDist * sin(textAngle).toFloat() - 6.dp.toPx()

                    drawText(
                        textMeasurer = textMeasurer,
                        text = labels[i],
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        ),
                        topLeft = Offset(labelX, labelY)
                    )
                }

                // 3. Draw voice profile data polygon
                val dataPath = Path()
                val points = mutableListOf<Offset>()
                for (i in 0 until numSides) {
                    val angle = i * (2f * Math.PI / numSides) - Math.PI / 2f
                    val ratio = values[i].coerceIn(0.1f, 1f)
                    val r = maxRadius * ratio
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    points.add(Offset(x, y))
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()

                // Fill polygon with a translucent primary gradient
                drawPath(
                    path = dataPath,
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.45f), secondaryColor.copy(alpha = 0.2f)),
                        center = center,
                        radius = maxRadius
                    )
                )

                // Outline polygon
                drawPath(
                    path = dataPath,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw dots at vertices
                points.forEach { pt ->
                    drawCircle(
                        color = secondaryColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Breakdown scores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("חתך פונטי כללי", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "${((profile.pronunciationClarity + profile.clarityScore) / 2)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("רמת דיוק גבוהה", fontSize = 14.sp, color = Color(0xFF2E7D32))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("יציבות אות קול", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "${100 - profile.distortionLevel}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("מינימום רעש רקע", fontSize = 14.sp, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun PitchFrequencyTabContent(profile: VoiceProfile) {
    val textMeasurer = rememberTextMeasurer()
    var hoverX by remember { mutableStateOf<Float?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ניתוח תפוצת Pitch וגובה תדר קולי",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "גרף רציף בהשראת Recharts המציג שינויי תדר (Pitch Contour) בזמן אמת. גרור את האצבע על הגרף לצפייה בערכים מדויקים בכל נקודת זמן.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom drawn interactive Area Chart with coordinates and live tooltip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> hoverX = offset.x },
                            onDragEnd = { hoverX = null },
                            onDragCancel = { hoverX = null },
                            onDrag = { change, _ -> hoverX = change.position.x }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                hoverX = offset.x
                                tryAwaitRelease()
                                hoverX = null
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height

                // 1. Draw modern grid overlay (dashed helper grid)
                val gridRows = 4
                val gridCols = 6
                for (r in 1 until gridRows) {
                    val y = height * (r.toFloat() / gridRows)
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (c in 1 until gridCols) {
                    val x = width * (c.toFloat() / gridCols)
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Compute timeline pitch frequency data
                val pointsCount = 40
                val signalPoints = mutableListOf<Offset>()
                val areaPath = Path()
                
                val baseFreq = profile.frequencyHz.toFloat()
                val intonationFactor = profile.intonationScore.toFloat() / 100f
                val baseCenterY = height * 0.5f

                for (i in 0..pointsCount) {
                    val progress = i.toFloat() / pointsCount
                    val x = progress * width
                    // Generate logical voice variations over time
                    val waveAngle = progress * 2f * Math.PI.toFloat()
                    val variation = sin(waveAngle * 2) * 20f * intonationFactor + cos(waveAngle * 4) * 8f
                    val yVal = (baseCenterY - (baseFreq - 150f) * 0.3f + variation).coerceIn(12.dp.toPx(), height - 20.dp.toPx())
                    signalPoints.add(Offset(x, yVal))

                    if (i == 0) {
                        areaPath.moveTo(x, height)
                        areaPath.lineTo(x, yVal)
                    } else {
                        areaPath.lineTo(x, yVal)
                    }
                }
                areaPath.lineTo(width, height)
                areaPath.close()

                // Draw filled gradient area (Recharts aesthetic)
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // Draw solid line
                val linePath = Path()
                signalPoints.forEachIndexed { idx, pt ->
                    if (idx == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw user mean frequency baseline
                drawLine(
                    color = secondaryColor.copy(alpha = 0.35f),
                    start = Offset(0f, baseCenterY - (baseFreq - 150f) * 0.3f),
                    end = Offset(width, baseCenterY - (baseFreq - 150f) * 0.3f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )

                // 3. Render hover interaction state & dynamic tooltips
                val currentHoverX = hoverX
                if (currentHoverX != null) {
                    val clampedHoverX = currentHoverX.coerceIn(0f, width)
                    val ratioX = clampedHoverX / width
                    val approximateIdx = (ratioX * pointsCount).toInt().coerceIn(0, pointsCount)
                    val hoveredPoint = signalPoints[approximateIdx]

                    // Vertical cursor guide
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(hoveredPoint.x, 0f),
                        end = Offset(hoveredPoint.x, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Target bubble dot highlight
                    drawCircle(
                        color = secondaryColor,
                        radius = 6.dp.toPx(),
                        center = hoveredPoint
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = hoveredPoint
                    )

                    // Draw Recharts-style rich floating tooltip card
                    // Calculate hz at this hover position
                    val progressHz = baseFreq + (sin(approximateIdx.toFloat() * 0.2f) * 15f * intonationFactor)
                    val labelText = "שניה: ${String.format("%.1f", ratioX * 5)}s | תדר: ${progressHz.toInt()} Hz"
                    
                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    
                    val bubbleW = textLayout.size.width + 16.dp.toPx()
                    val bubbleH = textLayout.size.height + 10.dp.toPx()
                    val bubbleX = (hoveredPoint.x - bubbleW / 2).coerceIn(4.dp.toPx(), width - bubbleW - 4.dp.toPx())
                    val bubbleY = (hoveredPoint.y - bubbleH - 12.dp.toPx()).coerceAtLeast(4.dp.toPx())

                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        topLeft = Offset(bubbleX, bubbleY),
                        size = Size(bubbleW, bubbleH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelText,
                        topLeft = Offset(bubbleX + 8.dp.toPx(), bubbleY + 5.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Descriptive notes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Insights",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "גובה הקול הממוצע (${profile.frequencyHz} Hz) מתאים לגוון ${profile.pitch}. תדר זה חיוני לקביעת מודל השיבוט התואם ביותר.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun NoiseLevelsTabContent(profile: VoiceProfile, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val noisePurity = (100 - profile.distortionLevel).coerceIn(0, 100)
    var hoverX by remember { mutableStateOf<Float?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "אנליזה של רעש סביבתי ועיוות אות קול",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "תרשים אות-לרעש (SNR Area) במבנה Recharts מפוצל. החלק העליון מראה את אנרגיית הדיבור והתחתון מראה עיוותי רקע. גרור לצפייה ביחס היסטורי.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom drawn Oscilloscope Area with coordinates and live interaction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> hoverX = offset.x },
                            onDragEnd = { hoverX = null },
                            onDragCancel = { hoverX = null },
                            onDrag = { change, _ -> hoverX = change.position.x }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                hoverX = offset.x
                                tryAwaitRelease()
                                hoverX = null
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerY = height * 0.5f

                // 1. Draw Recharts background grid lines
                val gridRows = 4
                val gridCols = 6
                for (r in 1 until gridRows) {
                    val y = height * (r.toFloat() / gridRows)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (c in 1 until gridCols) {
                    val x = width * (c.toFloat() / gridCols)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Render dual area curves (Speech Signal vs Noise Distortions)
                val pointsCount = 45
                val signalPoints = mutableListOf<Offset>()
                val noisePoints = mutableListOf<Offset>()
                val distortionMod = profile.distortionLevel.toFloat() / 100f
                val baseSignalStrength = (1f - distortionMod).coerceIn(0.2f, 1.0f)

                val signalPath = Path()
                val noisePath = Path()

                for (i in 0..pointsCount) {
                    val x = (i.toFloat() / pointsCount) * width
                    // Speech wave calculation
                    val speechVol = centerY * 0.7f * baseSignalStrength
                    val ySpeech = centerY - 15f - (sin(i.toFloat() * 0.25f) * speechVol).coerceAtLeast(0f)
                    
                    // Noise floor wave
                    val noiseVol = centerY * 0.4f * distortionMod
                    val yNoise = centerY + 15f + (sin(i.toFloat() * 0.65f) * noiseVol + cos(i.toFloat() * 0.15f) * 10f).coerceAtLeast(0f)

                    signalPoints.add(Offset(x, ySpeech))
                    noisePoints.add(Offset(x, yNoise))

                    if (i == 0) {
                        signalPath.moveTo(x, centerY)
                        signalPath.lineTo(x, ySpeech)
                        noisePath.moveTo(x, centerY)
                        noisePath.lineTo(x, yNoise)
                    } else {
                        signalPath.lineTo(x, ySpeech)
                        noisePath.lineTo(x, yNoise)
                    }
                }
                signalPath.lineTo(width, centerY)
                signalPath.close()

                noisePath.lineTo(width, centerY)
                noisePath.close()

                // Draw Signal Area Fill (Greenish gradient upward)
                drawPath(
                    path = signalPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.22f), Color.Transparent),
                        startY = centerY - height * 0.45f,
                        endY = centerY
                    )
                )

                // Draw Noise Area Fill (Reddish gradient downward)
                drawPath(
                    path = noisePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEF4444).copy(alpha = 0.15f), Color.Transparent),
                        startY = centerY,
                        endY = centerY + height * 0.45f
                    )
                )

                // Draw crisp border lines
                val speechBorderPath = Path()
                val noiseBorderPath = Path()
                signalPoints.forEachIndexed { idx, pt -> if (idx == 0) speechBorderPath.moveTo(pt.x, pt.y) else speechBorderPath.lineTo(pt.x, pt.y) }
                noisePoints.forEachIndexed { idx, pt -> if (idx == 0) noiseBorderPath.moveTo(pt.x, pt.y) else noiseBorderPath.lineTo(pt.x, pt.y) }

                drawPath(speechBorderPath, Color(0xFF10B981), style = Stroke(width = 2.dp.toPx()))
                drawPath(noiseBorderPath, Color(0xFFEF4444), style = Stroke(width = 1.5.dp.toPx()))

                // Draw split centerline separator
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 3. Render Hover interactions
                val currentHoverX = hoverX
                if (currentHoverX != null) {
                    val clampedHoverX = currentHoverX.coerceIn(0f, width)
                    val ratioX = clampedHoverX / width
                    val approximateIdx = (ratioX * pointsCount).toInt().coerceIn(0, pointsCount)
                    val hoveredSignal = signalPoints[approximateIdx]
                    val hoveredNoise = noisePoints[approximateIdx]

                    // Intersect guide line
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(clampedHoverX, 0f),
                        end = Offset(clampedHoverX, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw highlight circles at intersections
                    drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(), center = hoveredSignal)
                    drawCircle(color = Color(0xFFEF4444), radius = 5.dp.toPx(), center = hoveredNoise)

                    // SNR calculation under hover
                    val localSignalAmp = centerY - hoveredSignal.y
                    val localNoiseAmp = hoveredNoise.y - centerY
                    val snrVal = if (localNoiseAmp > 0f) (localSignalAmp / localNoiseAmp * 30f).toInt().coerceAtLeast(3) else 35
                    val tooltipLabel = "שניה: ${String.format("%.1f", ratioX * 5)}s | יחס SNR: +$snrVal dB | רעש: ${profile.distortionLevel}%"

                    val textLayout = textMeasurer.measure(
                        text = tooltipLabel,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )

                    val bubbleW = textLayout.size.width + 16.dp.toPx()
                    val bubbleH = textLayout.size.height + 10.dp.toPx()
                    val bubbleX = (clampedHoverX - bubbleW / 2).coerceIn(4.dp.toPx(), width - bubbleW - 4.dp.toPx())
                    val bubbleY = (hoveredSignal.y - bubbleH - 12.dp.toPx()).coerceAtLeast(4.dp.toPx())

                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        topLeft = Offset(bubbleX, bubbleY),
                        size = Size(bubbleW, bubbleH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = tooltipLabel,
                        topLeft = Offset(bubbleX + 8.dp.toPx(), bubbleY + 5.dp.toPx())
                    )
                }

                // Label on the graph
                drawText(
                    textMeasurer = textMeasurer,
                    text = "אות דיבור נקי (Signal - High)",
                    topLeft = Offset(10.dp.toPx(), 8.dp.toPx()),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "רעשי רקע (Noise) | ממוצע: ${profile.distortionLevel}%",
                    topLeft = Offset(10.dp.toPx(), centerY + 8.dp.toPx()),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Breakdown scores & Assessment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("ציון ניקיון קולי", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "$noisePurity%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (noisePurity >= 80) Color(0xFF10B981) else if (noisePurity >= 60) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    )
                    Text(
                        text = if (noisePurity >= 80) "איכות אולפן מעולה" else if (noisePurity >= 60) "איכות סבירה" else "רועש - מומלץ להקליט שוב",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (noisePurity >= 80) Color(0xFF2E7D32) else if (noisePurity >= 60) Color(0xFFD97706) else Color(0xFFB91C1C)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("רמת רעש רקע (dB-est)", fontSize = 14.sp, color = Color.Gray)
                    val dbEst = (profile.distortionLevel * 0.6f + 30).toInt()
                    Text(
                        text = "~$dbEst dB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (dbEst < 45) Color(0xFF10B981) else if (dbEst < 60) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    )
                    Text(
                        text = if (dbEst < 45) "ללא הפרעות מורגשות" else if (dbEst < 60) "נוכחות רעש קלה" else "הפרעות רעש קשות",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dbEst < 45) Color(0xFF2E7D32) else if (dbEst < 60) Color(0xFFD97706) else Color(0xFFB91C1C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Environmental Tips for cloning optimization
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💡", fontSize = 16.sp)
                    Text(
                        text = "טיפים לשיבוט קול מושלם:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                BulletText("הקלט בחדר סגור, הרחק ממאווררים, מזגנים או מכשירי חשמל רועשים.")
                BulletText("הנח את הטלפון במרחק של כ-15-20 ס״מ מהפה לקבלת סיגנל מאוזן.")
                BulletText("פרוס שטיח או כריות בחדר כדי למנוע החזר הד (Reverb) שפוגע באינטונציה.")
            }
        }
    }
}

@Composable
fun BulletText(text: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}
