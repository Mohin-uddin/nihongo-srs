package com.nihongo.srs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Design system.
 * Indigo (藍, ai) is the main colour, a sun red (日の丸) is the accent,
 * on a cool off-white background with white cards.
 */
private val Indigo = Color(0xFF3D4ED8)
private val IndigoDeep = Color(0xFF26318F)
private val IndigoSoft = Color(0xFFE7EAFF)
private val SunRed = Color(0xFFE5484D)
private val SunRedSoft = Color(0xFFFFE5E5)
private val Ink = Color(0xFF161A2E)
private val InkMuted = Color(0xFF61667D)
private val Paper = Color(0xFFF4F5FA)
private val Line = Color(0xFFE3E5EF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = IndigoDeep,
    secondary = SunRed,
    onSecondary = Color.White,
    secondaryContainer = SunRedSoft,
    onSecondaryContainer = Color(0xFF7A1418),
    tertiary = Color(0xFF0E9F6E),
    tertiaryContainer = Color(0xFFDDF7EC),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFF1F8),
    onSurfaceVariant = InkMuted,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFF0F2F9),
    surfaceContainerHighest = Color(0xFFE9ECF6),
    outline = Color(0xFFC9CCDA),
    outlineVariant = Line,
    error = SunRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AA6FF),
    onPrimary = Color(0xFF111A66),
    primaryContainer = Color(0xFF2A3488),
    onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFFFF8A8E),
    onSecondary = Color(0xFF5C0A0F),
    secondaryContainer = Color(0xFF5A1F23),
    onSecondaryContainer = Color(0xFFFFDADB),
    tertiary = Color(0xFF4ADE9F),
    tertiaryContainer = Color(0xFF0B3D2C),
    onTertiaryContainer = Color(0xFFC8F5E1),
    background = Color(0xFF0E1020),
    onBackground = Color(0xFFE8E9F3),
    surface = Color(0xFF181B2E),
    onSurface = Color(0xFFE8E9F3),
    surfaceVariant = Color(0xFF22263D),
    onSurfaceVariant = Color(0xFFA6A9BF),
    surfaceContainer = Color(0xFF181B2E),
    surfaceContainerLow = Color(0xFF181B2E),
    surfaceContainerHigh = Color(0xFF22263D),
    surfaceContainerHighest = Color(0xFF2B2F48),
    outline = Color(0xFF4A4E68),
    outlineVariant = Color(0xFF2B2F48),
    error = Color(0xFFFF8A8E),
)

/** Colours outside Material's scheme: right / wrong answers and the hero gradient. */
@Immutable
data class ExtraColors(
    val correct: Color,
    val correctSoft: Color,
    val wrong: Color,
    val wrongSoft: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val streak: Color,
)

private val LightExtra = ExtraColors(
    correct = Color(0xFF0E9F6E), correctSoft = Color(0xFFDDF7EC),
    wrong = SunRed, wrongSoft = SunRedSoft,
    heroStart = Color(0xFF3D4ED8), heroEnd = Color(0xFF1E2577),
    streak = Color(0xFFF59E0B),
)

private val DarkExtra = ExtraColors(
    correct = Color(0xFF4ADE9F), correctSoft = Color(0xFF0B3D2C),
    wrong = Color(0xFFFF8A8E), wrongSoft = Color(0xFF5A1F23),
    heroStart = Color(0xFF3442B8), heroEnd = Color(0xFF161B55),
    streak = Color(0xFFFBBF24),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

/** Short access: `Extra.correct`. */
val Extra: ExtraColors
    @Composable get() = LocalExtraColors.current

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp),
    )
}

/** Big Japanese text on cards. */
val JapaneseDisplay = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 66.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun NihongoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtraColors provides if (darkTheme) DarkExtra else LightExtra
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
