package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============================================================================
// LUXURY CHRONOGRAPH WATCH TYPOGRAPHY
// Serif for elegant major numerals & chronometer headings
// Crisp geometric sans-serif for indices, sub-dial labels & schedule items
// ============================================================================

val ChronoSerifFamily = FontFamily.Serif
val ChronoSansFamily = FontFamily.SansSerif

val Typography = Typography(
  // Major Chronometer Display (Time, Completion %)
  displayLarge = TextStyle(
    fontFamily = ChronoSerifFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.5).sp,
    color = WarmOffWhite
  ),
  displayMedium = TextStyle(
    fontFamily = ChronoSerifFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.2).sp,
    color = WarmOffWhite
  ),
  displaySmall = TextStyle(
    fontFamily = ChronoSerifFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.sp,
    color = WarmOffWhite
  ),

  // Watch Dial Headings & Subdial Headers
  headlineLarge = TextStyle(
    fontFamily = ChronoSerifFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.2.sp,
    color = WarmOffWhite
  ),
  headlineMedium = TextStyle(
    fontFamily = ChronoSerifFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.2.sp,
    color = WarmOffWhite
  ),
  headlineSmall = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.5.sp,
    color = WarmOffWhite
  ),

  // Titles for Schedule Blocks & Cards
  titleLarge = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.3.sp,
    color = WarmOffWhite
  ),
  titleMedium = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.2.sp,
    color = WarmOffWhite
  ),
  titleSmall = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.4.sp,
    color = WarmParchment
  ),

  // Body Text for Task Descriptions & Settings
  bodyLarge = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.2.sp,
    color = WarmOffWhite
  ),
  bodyMedium = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.2.sp,
    color = WarmParchment
  ),
  bodySmall = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
    color = WarmMuted
  ),

  // Chronometer Bezel / Subdial Indices Labels (Uppercase tracking)
  labelLarge = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.0.sp,
    color = GoldBrass
  ),
  labelMedium = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp,
    color = WarmParchment
  ),
  labelSmall = TextStyle(
    fontFamily = ChronoSansFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.6.sp,
    color = WarmMuted
  )
)
