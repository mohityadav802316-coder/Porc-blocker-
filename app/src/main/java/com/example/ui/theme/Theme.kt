package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChronoDarkColorScheme = darkColorScheme(
  primary = GoldBrass,
  onPrimary = ObsidianCharcoal,
  primaryContainer = GoldMuted,
  onPrimaryContainer = GoldHighlight,

  secondary = SageGreen,
  onSecondary = ObsidianCharcoal,
  secondaryContainer = StatusCompleteContainer,
  onSecondaryContainer = WarmOffWhite,

  tertiary = IceBlue,
  onTertiary = ObsidianCharcoal,
  tertiaryContainer = ObsidianSurfaceElevated,
  onTertiaryContainer = IceBlueGlow,

  background = ObsidianCharcoal,
  onBackground = WarmOffWhite,

  surface = ObsidianCard,
  onSurface = WarmOffWhite,
  surfaceVariant = ObsidianElevated,
  onSurfaceVariant = WarmParchment,

  outline = ObsidianBorder,
  outlineVariant = ObsidianBorderSubtle,

  error = DustyRose,
  onError = ObsidianCharcoal,
  errorContainer = StatusMissedContainer,
  onErrorContainer = WarmOffWhite
)

@Composable
fun LifeTrackerTheme(
  darkTheme: Boolean = true, // Luxury obsidian precision instrument
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = ChronoDarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  LifeTrackerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
