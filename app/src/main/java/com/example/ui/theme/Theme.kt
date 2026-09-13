package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = GrassGreenPrimaryDark,
    onPrimary = GrassGreenOnPrimaryDark,
    primaryContainer = GrassGreenContainerDark,
    onPrimaryContainer = GrassGreenOnContainerDark,
    secondary = SproutSecondaryDark,
    onSecondary = SproutOnSecondaryDark,
    secondaryContainer = SproutSecondaryContainerDark,
    onSecondaryContainer = SproutOnSecondaryContainerDark,
    tertiary = Color(0xFFBCAAA4),
    background = NatureDarkBackground,
    surface = NatureDarkSurface,
    surfaceVariant = NatureDarkSurfaceVariant,
    onBackground = Color(0xFFE2EBE2),
    onSurface = Color(0xFFE2EBE2),
    onSurfaceVariant = Color(0xFFC0CCC1)
)

private val LightColorScheme = lightColorScheme(
    primary = GrassGreenPrimary,
    onPrimary = GrassGreenOnPrimary,
    primaryContainer = GrassGreenContainer,
    onPrimaryContainer = GrassGreenOnContainer,
    secondary = SproutSecondary,
    onSecondary = SproutOnSecondary,
    secondaryContainer = SproutSecondaryContainer,
    onSecondaryContainer = SproutOnSecondaryContainer,
    tertiary = EarthTertiary,
    background = NatureLightBackground,
    surface = NatureLightSurface,
    surfaceVariant = NatureLightSurfaceVariant,
    onBackground = Color(0xFF191D1A),
    onSurface = Color(0xFF191D1A),
    onSurfaceVariant = Color(0xFF404A41)
)

@Composable
fun TouchGrassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to cohesive brand nature greens
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    TouchGrassTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
