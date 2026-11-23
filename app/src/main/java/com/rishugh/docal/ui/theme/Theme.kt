package com.rishugh.docal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = SlateDarkTextPrimary,
    primaryContainer = SlateDarkCard,
    onPrimaryContainer = PrimaryIndigo,
    secondary = AccentPurple,
    tertiary = AccentTeal,
    background = SlateDarkBackground,
    onBackground = SlateDarkTextPrimary,
    surface = SlateDarkSurface,
    onSurface = SlateDarkTextPrimary,
    surfaceVariant = SlateDarkCard,
    onSurfaceVariant = SlateDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = SlateLightSurface,
    primaryContainer = SlateLightCard,
    onPrimaryContainer = PrimaryIndigoDark,
    secondary = AccentPurple,
    tertiary = AccentTeal,
    background = SlateLightBackground,
    onBackground = SlateLightTextPrimary,
    surface = SlateLightSurface,
    onSurface = SlateLightTextPrimary,
    surfaceVariant = SlateLightCard,
    onSurfaceVariant = SlateLightTextSecondary
)

@Composable
fun DoCalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
