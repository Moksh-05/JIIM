package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GymDarkColorScheme = darkColorScheme(
  primary = TitaniumWhite,
  onPrimary = MatteBlack,
  primaryContainer = CardElevated,
  onPrimaryContainer = TitaniumWhite,
  secondary = TitaniumSilver,
  onSecondary = MatteBlack,
  secondaryContainer = CardDark,
  onSecondaryContainer = TitaniumSilver,
  tertiary = PlatinumSteel,
  onTertiary = MatteBlack,
  background = MatteBlack,
  onBackground = TextPrimary,
  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = CardDark,
  onSurfaceVariant = TextSecondary,
  outline = BorderSubtle,
  outlineVariant = BorderHighlight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force modern dark athletic theme for gym lifters
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = GymDarkColorScheme,
    typography = Typography,
    content = content
  )
}
