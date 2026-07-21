package com.example.ui.theme

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
    primary = GlowingCopper,
    secondary = WarmGold,
    tertiary = MechCategory,
    background = CharcoalBg,
    surface = SlateCard,
    onPrimary = CharcoalBg,
    onSecondary = CharcoalBg,
    onBackground = TextLight,
    onSurface = TextLight,
    primaryContainer = DarkSteel,
    onPrimaryContainer = TextLight,
    surfaceVariant = DarkSteel,
    onSurfaceVariant = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = GlowingCopper,
    tertiary = MechCategory,
    background = LightBg,
    surface = LightCard,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextDark,
    onSurface = TextDark,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    onPrimaryContainer = TextDark,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    onSurfaceVariant = TextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color true if desired on Android 12+
    dynamicColor: Boolean = false, // Set to false to force our custom Industrial Copper theme which is extremely premium!
    content: @Composable () -> Unit,
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
