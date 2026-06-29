package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Light, Warm & Friendly Theme Colors (As requested: "עיצוב בהיר יותר ונעים לעיין, רקע פחות כהה וקודר")
val LightBg = Color(0xFFF9FAFC)        // Warm off-white, pleasant to the eye
val LightSurface = Color(0xFFFFFFFF)   // Pure white for card components
val LightPrimary = Color(0xFF6366F1)   // Warm modern Indigo
val BrandNavy = Color(0xFF162544)      // Dark Navy for buttons
val LightSecondary = Color(0xFFEC4899) // Soft pleasant Rose/Pink
val LightTertiary = Color(0xFF14B8A6)  // Friendly Soft Teal
val DarkCharcoal = Color(0xFF1F2937)   // Clean rounded charcoal for ultra-clear text
val SoftMuted = Color(0xFF6B7280)      // Soft grey for secondary labels
val LightBorder = Color(0xFFEEF2F6)    // Extremely clean light-border color
val LightGreen = Color(0xFF10B981)     // Warm bright success green

// Premium Pastel Gradient Brush (Smooth blending of lavender, rose, warm peach, and sky blue)
val PastelGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFEEF2F6), // Base light warm off-white
        Color(0xFFE0E7FF), // Soft lavender indigo-pastel
        Color(0xFFFCE7F3), // Soft baby rose pink
        Color(0xFFFEF3C7), // Soft sunny yellow/peach
        Color(0xFFE0F2FE)  // Soft sky blue
    )
)

// Original Dark Theme Colors (kept for compatibility)
val CharcoalDark = Color(0xFF12131A)
val MatteNavy = Color(0xFF1E202C)
val SynthPurple = Color(0xFF8F8AD2)
val RetroPink = Color(0xFFFF729F)
val SoftWhite = Color(0xFFE5E7EB)
val MutedSlate = Color(0xFF9CA3AF)
val AccentCyan = Color(0xFF26C6DA)
val DarkGreyGlass = Color(0x331E202C)
val LightGreyGlass = Color(0x1Affffff)

