package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GymDarkColorScheme = darkColorScheme(
  primary = VoltLime,
  onPrimary = Color(0xFF0D1300),
  primaryContainer = Color(0xFF263300),
  onPrimaryContainer = VoltLime,
  secondary = ElectricCyan,
  onSecondary = Color(0xFF001F24),
  secondaryContainer = Color(0xFF00404A),
  onSecondaryContainer = ElectricCyan,
  tertiary = FlameOrange,
  onTertiary = Color.White,
  background = DarkObsidianBg,
  onBackground = TextWhite,
  surface = DarkSurface,
  onSurface = TextWhite,
  surfaceVariant = DarkCard,
  onSurfaceVariant = TextMuted,
  outline = DarkBorder,
  outlineVariant = Color(0xFF1B2433)
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
