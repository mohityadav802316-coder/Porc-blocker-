package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// LUXURY CHRONOGRAPH WATCH DESIGN PALETTE ("Precision Instrument")
// ============================================================================

// Warm Obsidian Charcoal Surfaces
val ObsidianCharcoal = Color(0xFF14120E)        // #14120E Primary Background
val ObsidianElevated = Color(0xFF1A1713)        // Elevated Dial/Plate
val ObsidianCard = Color(0xFF221E18)            // Brushed Subdial Card
val ObsidianSurfaceElevated = Color(0xFF2B251E) // Higher Elevation
val ObsidianSurfaceHighlight = Color(0xFF373027)// Inset Bezel / Active Plate
val ObsidianBorder = Color(0xFF383126)          // Hairline Brass/Charcoal Border
val ObsidianBorderSubtle = Color(0xFF27221A)    // Very Subtle Divider
val ObsidianTickMark = Color(0xFF453D30)        // Chrono Bezel Tick Mark

// Primary Accent: Luxury Brass / Horological Gold
val GoldBrass = Color(0xFFC9A15B)               // #C9A15B Classic Brass/Gold
val GoldHighlight = Color(0xFFDFBE7E)           // Champagne Gold Highlight
val GoldDark = Color(0xFF8B6F39)                // Deep Bronze Gold
val GoldMuted = Color(0xFF5A4826)               // Muted Antique Gold
val GoldSubtleSurface = Color(0x1FC9A15B)       // 12% Gold Tint for Surfaces

// Warm Typography Hierarchy
val WarmOffWhite = Color(0xFFF3EEDD)            // #F3EEDD Primary Text
val WarmParchment = Color(0xFFBDB49F)           // Secondary Off-White
val WarmMuted = Color(0xFF857D6C)               // Muted Subtitle / Indices
val WarmDisabled = Color(0xFF554F43)            // Disabled Text

// Supporting Instrument Colors (Muted, Sophisticated, Horological)
val SageGreen = Color(0xFF7D9B76)               // Completed / In-Sync / Target Met
val SageGreenGlow = Color(0xFF96B88E)
val SageGreenSurface = Color(0x1F7D9B76)

val DustyRose = Color(0xFFC87D7D)               // Missed / Alert
val DustyRoseGlow = Color(0xFFDE9595)
val DustyRoseSurface = Color(0x1FC87D7D)

val MutedPlum = Color(0xFF96789C)               // Partial / Evening / Reflection
val MutedPlumGlow = Color(0xFFB292B8)
val MutedPlumSurface = Color(0x1F96789C)

val IceBlue = Color(0xFF7AA2BA)                 // Focus / Water / Calm Indices
val IceBlueGlow = Color(0xFF96BDD4)
val IceBlueSurface = Color(0x1F7AA2BA)

// Backward-Compatible Semantic Tokens (Mapped to Luxury Palette - No Neon!)
val DarkBackground = ObsidianCharcoal
val DarkBackgroundElevated = ObsidianElevated
val DarkSurface = ObsidianCard
val DarkSurfaceElevated = ObsidianSurfaceElevated
val DarkSurfaceHighlight = ObsidianSurfaceHighlight
val DarkSurfaceBorder = ObsidianBorder
val DarkSurfaceBorderSubtle = ObsidianBorderSubtle

val CyanNeon = GoldBrass                       // Replaced electric cyan with Gold/Brass
val CyanGlow = GoldHighlight
val CyanDim = GoldDark
val CyanMuted = GoldMuted

val VioletNeon = MutedPlum                     // Replaced violet neon with Muted Plum
val VioletDeep = ObsidianElevated
val VioletGlow = GoldHighlight

val BlueAccent = IceBlue
val BlueGlow = IceBlueGlow

val StatusComplete = SageGreen                 // Sage Green
val StatusCompleteGlow = SageGreenGlow
val StatusCompleteContainer = Color(0xFF263324)
val StatusCompleteSurface = SageGreenSurface

val StatusPartial = MutedPlum                  // Muted Plum
val StatusPartialGlow = MutedPlumGlow
val StatusPartialContainer = Color(0xFF332536)
val StatusPartialSurface = MutedPlumSurface

val StatusMissed = DustyRose                   // Dusty Rose
val StatusMissedGlow = DustyRoseGlow
val StatusMissedContainer = Color(0xFF382323)
val StatusMissedSurface = DustyRoseSurface

val ExtraTaskAccent = GoldHighlight
val ExtraTaskGlow = GoldBrass
val ExtraTaskContainer = ObsidianElevated
val ExtraTaskSurface = GoldSubtleSurface

val TextPrimary = WarmOffWhite
val TextSecondary = WarmParchment
val TextMuted = WarmMuted
val TextDisabled = WarmDisabled

// Chronograph Instrument Gradients
val GlassGradient = Brush.verticalGradient(
  colors = listOf(
    ObsidianElevated.copy(alpha = 0.95f),
    ObsidianCharcoal.copy(alpha = 0.98f)
  )
)

val CardBorderGradient = Brush.linearGradient(
  colors = listOf(
    GoldBrass.copy(alpha = 0.45f),
    ObsidianBorder,
    GoldDark.copy(alpha = 0.25f)
  )
)

val ActiveCardBorderGradient = Brush.linearGradient(
  colors = listOf(
    GoldHighlight,
    GoldBrass,
    GoldDark
  )
)

val ExtraTaskBorderGradient = Brush.linearGradient(
  colors = listOf(
    GoldHighlight.copy(alpha = 0.6f),
    GoldBrass.copy(alpha = 0.4f),
    ObsidianBorder
  )
)

val ChronoDialGradient = Brush.sweepGradient(
  colors = listOf(
    GoldDark,
    GoldBrass,
    GoldHighlight,
    GoldBrass,
    GoldDark
  )
)
