package com.memorycare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OrbitalCyan,
    onPrimary = DeepSpaceNavy,
    primaryContainer = SpaceNavyCard,
    onPrimaryContainer = OrbitalCyan,
    secondary = SatelliteGreen,
    onSecondary = DeepSpaceNavy,
    background = DeepSpaceNavy,
    onBackground = ElderlySurfaceWhite,
    surface = SpaceNavySurface,
    onSurface = ElderlySurfaceWhite,
    error = CriticalRed,
    onError = ElderlySurfaceWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ActionPrimaryBlue,
    onPrimary = ElderlySurfaceWhite,
    primaryContainer = ActionCardBackground,
    onPrimaryContainer = ActionPrimaryBlueDark,
    secondary = ActionSuccessGreen,
    onSecondary = ElderlySurfaceWhite,
    background = ElderlyBgLight,
    onBackground = ElderlyTextPrimary,
    surface = ElderlySurfaceWhite,
    onSurface = ElderlyTextPrimary,
    error = ActionWarningOrange,
    onError = ElderlySurfaceWhite
)

@Composable
fun MemoryCareTheme(
    darkTheme: Boolean = false, // Keep default high contrast light theme for maximum elderly readability
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Top status bar uses Space Navy for the space-tech theme
                window.statusBarColor = DeepSpaceNavy.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElderlyTypography,
        content = content
    )
}
