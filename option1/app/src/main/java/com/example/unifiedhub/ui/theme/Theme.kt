// =============================================================================
// Theme.kt — The "look and feel" of our app
// =============================================================================
// WHY: A theme defines colors, fonts, and shapes used throughout the app.
// Instead of hardcoding colors in every screen, we define them ONCE here
// and use them everywhere. This makes it easy to change the look later.
//
// WHAT IS Material3?
// Material Design 3 is Google's design system. It provides ready-made
// components (buttons, cards, etc.) that look professional and follow
// Android design guidelines.
// =============================================================================

package com.example.unifiedhub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// --- Define our color palettes ---
// WHY two palettes? Users can switch between light and dark mode.
// We want our app to look good in both.

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1976D2),      // Blue — main brand color
    secondary = androidx.compose.ui.graphics.Color(0xFF43A047),    // Green — accent
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF8F00),     // Amber — highlights
    background = androidx.compose.ui.graphics.Color(0xFFF5F5F5),   // Light gray background
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),      // White cards/surfaces
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    secondary = androidx.compose.ui.graphics.Color(0xFF81C784),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFFCC80),
)

// --- The main theme function ---
// WHY @Composable? Because it's a Compose function that provides theme
// data to all the UI elements inside it.

@Composable
fun UnifiedHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // Auto-detect dark mode
    content: @Composable () -> Unit               // The UI inside the theme
) {
    // Pick the right color scheme based on dark/light mode
    // On Android 12+, we can use "dynamic colors" that match the user's wallpaper
    val colorScheme = when {
        // Dynamic colors (Android 12+) — matches user's wallpaper colors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // MaterialTheme wraps our entire app and provides colors/fonts to all children
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),  // Default Material3 typography
        content = content
    )
}
