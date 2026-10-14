package com.nihongo.srs.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Japanese-flag red + Bangladesh-flag green.
private val HinomaruRed = Color(0xFFBC002D)
private val BangladeshGreen = Color(0xFF006A4E)

private val LightColors = lightColorScheme(
    primary = HinomaruRed,
    secondary = BangladeshGreen,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3B5),
    secondary = Color(0xFF7FD8B5),
)

/** Correct / wrong button colours used on the flashcard screen. */
val CorrectGreen = BangladeshGreen
val ForgotRed = HinomaruRed

@Composable
fun NihongoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
