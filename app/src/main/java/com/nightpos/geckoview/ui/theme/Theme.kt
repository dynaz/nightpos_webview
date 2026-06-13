package com.nightpos.geckoview.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * NightPOS is intentionally a dark-only theme — bright UIs are unsuitable for the
 * low-light restaurant/nightclub environments this POS targets, and a single fixed
 * palette guarantees consistent contrast/branding across all devices.
 */
private val NightPOSDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = NightBlack,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = NeonVioletLight,
    secondary = NeonPink,
    onSecondary = NightBlack,
    secondaryContainer = NightSurfaceVariant,
    onSecondaryContainer = NeonVioletLight,
    tertiary = AmberAccent,
    onTertiary = NightBlack,
    background = NightBlack,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonPurple,
    error = ErrorRed,
    onError = NightBlack,
    outline = NightSurfaceBright,
)

@Composable
fun NightPOSTheme(
    // Dynamic color is opt-in: the brand palette is core to NightPOS's identity.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)
        else -> NightPOSDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NightPOSTypography,
        content = content,
    )
}

/** Convenience accessor mirroring [isSystemInDarkTheme] — NightPOS is always dark. */
val isNightPOSDark: Boolean
    @Composable get() = true
