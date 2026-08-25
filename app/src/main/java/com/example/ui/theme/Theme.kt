package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProfessionalDarkColorScheme = darkColorScheme(
  primary = PrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF1E3A8A),
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = MarineCyan,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF0369A1),
  onSecondaryContainer = Color(0xFFE0F2FE),
  tertiary = SeaGreen,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFF064E3B),
  onTertiaryContainer = Color(0xFFA7F3D0),
  background = HeaderNavy,
  onBackground = TextWhite,
  surface = Color(0xFF1E293B),
  onSurface = TextWhite,
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF475569),
  outlineVariant = Color(0xFF334155),
  error = DangerRed,
  onError = Color.White,
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFECACA)
)

private val ProfessionalLightColorScheme = lightColorScheme(
  primary = PrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = PrimaryBlueLight,
  onPrimaryContainer = PrimaryBlueDark,
  secondary = MarineCyan,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = SeaGreen,
  onTertiary = Color.White,
  tertiaryContainer = SeaGreenLight,
  onTertiaryContainer = SeaGreen,
  background = BackgroundCanvas,
  onBackground = TextPrimary,
  surface = CardWhite,
  onSurface = TextPrimary,
  surfaceVariant = CardSubtle,
  onSurfaceVariant = TextSecondary,
  outline = CardSubtleBorder,
  outlineVariant = CardBorder,
  error = DangerRed,
  onError = Color.White,
  errorContainer = DangerRedLight,
  onErrorContainer = DangerRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Clean, high-precision Professional Polish theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ProfessionalDarkColorScheme else ProfessionalLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

