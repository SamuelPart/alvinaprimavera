package com.flippy.walkietalkie.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FlippyDarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    primaryContainer = AccentContainer,
    onPrimaryContainer = Accent,
    secondary = AccentBlue,
    onSecondary = Background,
    secondaryContainer = AccentBlueContainer,
    onSecondaryContainer = AccentBlue,
    tertiary = WarningAmber,
    error = RecordRed,
    onError = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = OutlineLight
)

@Composable
fun FlippyTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = FlippyDarkScheme,
        typography = Typography,
        content = content
    )
}
