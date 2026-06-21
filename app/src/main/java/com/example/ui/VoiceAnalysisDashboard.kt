package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
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
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ניתוח תפוצת Pitch & גובה תדר קולי",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "דיאגרמת התפוצה להלן מראה היכן תדר הדיבור הממוצע של הקול (${profile.frequencyHz} Hz) ממוקם בהתפלגות התדרים האנושית הסטנדרטית.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Gaussian Distribution bell curve with target marker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // 1. Draw horizontal axis guideline
                val axisY = height * 0.85f
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(0f, axisY),
                    end = Offset(width, axisY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 2. Draw standard voice frequency zones underneath
                // Bass/Baritone: 80Hz - 160Hz
                // Tenor/Alto: 160Hz - 210Hz
                // Soprano: 210Hz - 280Hz
                val zone1Width = width * 0.4f
                val zone2Width = width * 0.35f
                val zone3Width = width * 0.25f

                // Draw colored zone backgrounds
                drawRect(
                    color = Color(0x0C3B82F6), // Blue-ish for low ranges
                    topLeft = Offset(0f, axisY),
                    size = Size(zone1Width, 16.dp.toPx())
                )
                drawRect(
                    color = Color(0x0C10B981), // Green-ish for mid ranges
                    topLeft = Offset(zone1Width, axisY),
                    size = Size(zone2Width, 16.dp.toPx())
                )
                drawRect(
                    color = Color(0x0CEF4444), // Pink-ish for high ranges
                    topLeft = Offset(zone1Width + zone2Width, axisY),
                    size = Size(zone3Width, 16.dp.toPx())
                )

                // Label zones
                drawText(
                    textMeasurer = textMeasurer,
                    text = "גברים (בס/בריטון)",
                    topLeft = Offset(10.dp.toPx(), axisY + 2.dp.toPx()),
                    style = TextStyle(fontSize = 14.sp, color = Color(0xFF475569))
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "אלטו / אמצע",
                    topLeft = Offset(zone1Width + 10.dp.toPx(), axisY + 2.dp.toPx()),
                    style = TextStyle(fontSize = 14.sp, color = Color(0xFF475569))
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "נשים (סופרן)",
                    topLeft = Offset(zone1Width + zone2Width + 10.dp.toPx(), axisY + 2.dp.toPx()),
                    style = TextStyle(fontSize = 14.sp, color = Color(0xFF475569))
                )

                // 3. Draw a smooth double bell curves representing distributions (Male and Female)
                // Left curve (Male): Peak around width * 0.3
                // Right curve (Female): Peak around width * 0.65
                val pathMale = Path()
                val pathFemale = Path()
                
                val steps = 50
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * width
                    
                    // Male Gaussian bell distribution
                    val distM = (x - width * 0.32f) / (width * 0.12f)
                    val yM = axisY - (kotlin.math.exp(-0.5f * distM * distM) * height * 0.65f)
                    
                    // Female Gaussian bell distribution
                    val distF = (x - width * 0.68f) / (width * 0.12f)
                    val yF = axisY - (kotlin.math.exp(-0.5f * distF * distF) * height * 0.55f)

                    if (i == 0) {
                        pathMale.moveTo(x, yM)
                        pathFemale.moveTo(x, yF)
                    } else {
                        pathMale.lineTo(x, yM)
                        pathFemale.lineTo(x, yF)
                    }
                }

                drawPath(pathMale, Color(0xFF3B82F6).copy(alpha = 0.25f), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                drawPath(pathFemale, Color(0xFFEC4899).copy(alpha = 0.25f), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))

                // 4. Highlight User's frequency index position
                // Map user Hz (60 Hz to 280 Hz) to X coordinate (0% to 100% width)
                val userHzNormalized = ((profile.frequencyHz - 60f) / 220f).coerceIn(0f, 1f)
                val userX = userHzNormalized * width
                
                // Calculate height of curve at userX (approximate blend of curves for beautiful visuals)
                val distUserM = (userX - width * 0.32f) / (width * 0.12f)
                val yUserM = axisY - (kotlin.math.exp(-0.5f * distUserM * distUserM) * height * 0.65f)
                val distUserF = (userX - width * 0.68f) / (width * 0.12f)
                val yUserF = axisY - (kotlin.math.exp(-0.5f * distUserF * distUserF) * height * 0.55f)
                
                // Active user position y is the higher of standard curve representation
                val userY = kotlin.math.min(yUserM, yUserF).coerceIn(height * 0.15f, axisY - 10.dp.toPx())

                // Draw vertical line from axis to point
                drawLine(
                    color = Color(0xFF6366F1),
                    start = Offset(userX, axisY),
                    end = Offset(userX, userY),
                    strokeWidth = 2.dp.toPx()
                )

                // Fill bubble marker
                drawCircle(
                    color = Color(0xFF6366F1),
                    radius = 7.dp.toPx(),
                    center = Offset(userX, userY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(userX, userY)
                )

                // Draw floating helper pill above user indicator
                val pText = "${profile.frequencyHz} Hz"
                val textLayoutResult = textMeasurer.measure(
                    text = pText,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black)
                )
                val bubbleW = textLayoutResult.size.width + 12.dp.toPx()
                val bubbleH = textLayoutResult.size.height + 6.dp.toPx()
                val bubbleX = (userX - bubbleW / 2).coerceIn(4.dp.toPx(), width - bubbleW - 4.dp.toPx())
                val bubbleY = userY - bubbleH - 6.dp.toPx()

                drawRoundRect(
                    color = Color(0xFF1E1E2E).copy(alpha = 0.85f),
                    topLeft = Offset(bubbleX, bubbleY),
                    size = Size(bubbleW, bubbleH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = pText,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    topLeft = Offset(bubbleX + 6.dp.toPx(), bubbleY + 3.dp.toPx())
                )
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "אנליזה של רעש סביבתי ועיוות אות קול",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "תרשים האוסצילוסקופ להלן מדגים את ההפרדה בין אות הדיבור הטהור לבין רעשי הרקע שנקלטו.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom drawn Oscilloscope for Signal-to-Noise Ratio (SNR)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height * 0.5f

                // 1. Draw zero-baseline reference grid line
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw helper grid gridlines
                val gridLines = 4
                for (i in 1 until gridLines) {
                    val x = width * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Draw standard signal waves
                val points = 60
                val pathVoice = Path()
                val pathNoise = Path()

                val distortionMod = profile.distortionLevel.toFloat() / 100f
                val signalStrength = (1f - distortionMod).coerceIn(0.2f, 1.0f)

                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width

                    // Sine Voice Wave (Smooth, clean, emerald green)
                    val voiceAmp = centerY * 0.7f * signalStrength
                    val voiceF = 0.15f
                    val yVoice = centerY + kotlin.math.sin(i.toFloat() * voiceF) * voiceAmp

                    // Aggressive jagged Background Noise representation (Choppy red wave)
                    val noiseAmp = centerY * 0.6f * distortionMod
                    val noiseF = 0.75f
                    val jaggedMultiplier = if (i % 2 == 0) 1.2f else 0.4f
                    val yNoise = centerY + kotlin.math.sin(i.toFloat() * noiseF) * noiseAmp * jaggedMultiplier

                    if (i == 0) {
                        pathVoice.moveTo(x, yVoice)
                        pathNoise.moveTo(x, yNoise)
                    } else {
                        pathVoice.lineTo(x, yVoice)
                        pathNoise.lineTo(x, yNoise)
                    }
                }

                // Draw background noise wave first
                if (profile.distortionLevel > 0) {
                    drawPath(
                        path = pathNoise,
                        color = Color(0xFFEF4444).copy(alpha = 0.65f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Draw clean speech signal wave on top
                drawPath(
                    path = pathVoice,
                    color = Color(0xFF10B981),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Label on the graph
                drawText(
                    textMeasurer = textMeasurer,
                    text = "אות דיבור (Signal)",
                    topLeft = Offset(10.dp.toPx(), 8.dp.toPx()),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "רעשי רקע (Noise): ${profile.distortionLevel}%",
                    topLeft = Offset(10.dp.toPx(), 26.dp.toPx()),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
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
