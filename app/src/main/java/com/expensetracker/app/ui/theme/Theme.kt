package com.expensetracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.ThemeMode

// Calm palette from the wireframes: deep teal for primary actions, amber for "needs attention".
//
// Deliberately NOT dynamic colour. Amber is semantic here, not decoration: it marks "needs
// attention" on the review banner, the peak bar, the swipe-to-dismiss background and the review
// chip. A wallpaper-derived scheme would scramble all four.
/**
 * Exposed because the PDF exporter draws on paper and therefore always uses the light palette.
 * It previously kept its own copy of these four values, which could drift silently.
 */
internal val LightScheme = lightColorScheme(
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
    // Just off the card white, so DropdownMenu/DatePickerDialog/AlertDialog have an
    // edge. The flat surfaceContainer* scheme is deliberate; this is the one exception.
    surfaceContainerHigh = Color(0xFFFBFAF6),
    outline = Color(0xFFD6D3CA),
    outlineVariant = Color(0xFFE3E1D9),
    inverseSurface = Color(0xFF17191A),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF8FD3C4),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
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

/** The one big number on the dashboard. Bespoke rather than a type-scale role, hence named here. */
val HeroAmount = AmountStyle.copy(fontSize = 40.sp)

/**
 * Onboarding page titles. One value: the three pages had drifted to 34sp, 32sp and 32sp for what
 * is the same role on consecutive screens.
 */
val OnboardingTitle = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 37.sp,
)

/**
 * The corner scale. Cards and the FAB are [large]; buttons are [extraLarge], which at the button
 * heights below is a full pill. Chips keep their own 18dp radius at the call site: they are a
 * different family of control and tracking the button scale would make them look like buttons.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Full-width commit actions at the foot of a screen or sheet: Continue, Save, Add, Share. */
val PrimaryButtonHeight = 56.dp

/**
 * Whether this mode paints dark. Public because the window itself has to agree with Compose:
 * the status-bar icons and the window background live outside the composition, and if they
 * follow the system night setting instead of this preference they contradict the app's colours.
 */
@Composable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ExpenseTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode.isDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
