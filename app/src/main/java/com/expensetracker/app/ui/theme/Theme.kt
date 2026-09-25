package com.expensetracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.ThemeMode

// Calm palette from the wireframes: deep teal for primary actions, amber for "needs attention".
private val Light = lightColorScheme(
    primary = Color(0xFF0E5A52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EEEB),
    onPrimaryContainer = Color(0xFF0A3F3A),
    secondary = Color(0xFF4A4F4F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEECE5),
    onSecondaryContainer = Color(0xFF17191A),
    tertiary = Color(0xFFD08A2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBEFD9),
    onTertiaryContainer = Color(0xFF6B4000),
    background = Color(0xFFF5F4EF),
    onBackground = Color(0xFF17191A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191A),
    surfaceVariant = Color(0xFFF0EFEA),
    onSurfaceVariant = Color(0xFF5B6060),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outline = Color(0xFFD6D3CA),
    outlineVariant = Color(0xFFE3E1D9),
    inverseSurface = Color(0xFF17191A),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF8FD3C4),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FCBBC),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1D4A43),
    onPrimaryContainer = Color(0xFFCDEBE4),
    secondary = Color(0xFFC3C8C7),
    onSecondary = Color(0xFF1B1E1E),
    secondaryContainer = Color(0xFF2A2F2F),
    onSecondaryContainer = Color(0xFFECEDEA),
    tertiary = Color(0xFFF2B766),
    onTertiary = Color(0xFF3F2800),
    tertiaryContainer = Color(0xFF4A3413),
    onTertiaryContainer = Color(0xFFFBE3B4),
    background = Color(0xFF121414),
    onBackground = Color(0xFFECEDEA),
    surface = Color(0xFF1B1E1E),
    onSurface = Color(0xFFECEDEA),
    surfaceVariant = Color(0xFF242828),
    onSurfaceVariant = Color(0xFFA9AFAE),
    surfaceContainerLow = Color(0xFF1B1E1E),
    surfaceContainer = Color(0xFF1B1E1E),
    surfaceContainerHigh = Color(0xFF222626),
    outline = Color(0xFF3A4040),
    outlineVariant = Color(0xFF2E3333),
    inverseSurface = Color(0xFF263030),
    inverseOnSurface = Color(0xFFF0F1EE),
    inversePrimary = Color(0xFF8FD3C4),
    error = Color(0xFFF2B8B5),
)

/** Serif for headings and big numbers, like the wireframes; system sans for everything else. */
val DisplayFamily: FontFamily = FontFamily.Serif

private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
        headlineSmall = headlineSmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold),
        displaySmall = displaySmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    )
}

val AmountStyle = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold)

@Composable
fun ExpenseTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
}
