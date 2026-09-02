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
  primary = Color(0xFF1D4ED8),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDBEAFE),
  onPrimaryContainer = Color(0xFF1E3A8A),
  secondary = Color(0xFF0284C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = Color(0xFF059669),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFD1FAE5),
  onTertiaryContainer = Color(0xFF065F46),
  background = Color(0xFFF1F5F9),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFF8FAFC),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE2E8F0),
  error = DangerRed,
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B)
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

