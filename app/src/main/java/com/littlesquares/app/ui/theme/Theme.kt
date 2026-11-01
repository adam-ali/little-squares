package com.littlesquares.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.ThemeMode

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

private val NunitoTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.nunito(), displayMedium = t.displayMedium.nunito(), displaySmall = t.displaySmall.nunito(),
        headlineLarge = t.headlineLarge.nunito(), headlineMedium = t.headlineMedium.nunito(), headlineSmall = t.headlineSmall.nunito(),
        titleLarge = t.titleLarge.nunito(), titleMedium = t.titleMedium.nunito(), titleSmall = t.titleSmall.nunito(),
        bodyLarge = t.bodyLarge.nunito(), bodyMedium = t.bodyMedium.nunito(), bodySmall = t.bodySmall.nunito(),
        labelLarge = t.labelLarge.nunito(), labelMedium = t.labelMedium.nunito(), labelSmall = t.labelSmall.nunito(),
    )
}

@Composable
fun LittleSquaresTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val ls = if (isDark(mode)) DarkLs else LightLs
    val lav = ls.pastel(Pastel.LAVENDER)
    val scheme = if (ls.dark) {
        darkColorScheme(
            primary = ls.primary, onPrimary = ls.onPrimary,
            primaryContainer = ls.sel, onPrimaryContainer = ls.ink,
            secondary = ls.ink2, onSecondary = ls.bg, secondaryContainer = ls.sel, onSecondaryContainer = ls.ink,
            tertiary = lav, onTertiary = ls.onPastel, tertiaryContainer = lav, onTertiaryContainer = ls.onPastel,
            background = ls.bg, onBackground = ls.ink, surface = ls.surface, onSurface = ls.ink,
            surfaceVariant = ls.surface2, onSurfaceVariant = ls.ink2,
            surfaceContainerLowest = ls.surface, surfaceContainerLow = ls.surface, surfaceContainer = ls.surface,
            surfaceContainerHigh = ls.surface, surfaceContainerHighest = ls.surface2,
            outline = ls.ink3, outlineVariant = ls.line, error = ls.danger,
        )
    } else {
        lightColorScheme(
            primary = ls.primary, onPrimary = ls.onPrimary,
            primaryContainer = ls.sel, onPrimaryContainer = ls.ink,
            secondary = ls.ink2, onSecondary = ls.bg, secondaryContainer = ls.sel, onSecondaryContainer = ls.ink,
            tertiary = lav, onTertiary = ls.onPastel, tertiaryContainer = lav, onTertiaryContainer = ls.onPastel,
            background = ls.bg, onBackground = ls.ink, surface = ls.surface, onSurface = ls.ink,
            surfaceVariant = ls.surface2, onSurfaceVariant = ls.ink2,
            surfaceContainerLowest = ls.surface, surfaceContainerLow = ls.surface, surfaceContainer = ls.surface,
            surfaceContainerHigh = ls.surface, surfaceContainerHighest = ls.surface2,
            outline = ls.ink3, outlineVariant = ls.line, error = ls.danger,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = NunitoTypography) {
        CompositionLocalProvider(
            LocalLs provides ls,
            LocalTextStyle provides ts(16, color = ls.ink),
        ) { content() }
    }
}
