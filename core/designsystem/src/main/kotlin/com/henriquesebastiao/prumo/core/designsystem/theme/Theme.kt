package com.henriquesebastiao.prumo.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.color.harmonize

/** How the app is drawn: what [PrumoTheme] was given, read by the [ModuleTheme]s inside it. */
@Immutable
data class ThemeSettings(val dark: Boolean, val dynamicColor: Boolean, val pureBlack: Boolean)

val LocalThemeSettings = staticCompositionLocalOf {
    ThemeSettings(dark = false, dynamicColor = false, pureBlack = false)
}

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors.of(Seeds.APP, dark = false) }

/**
 * Prumo's Material 3 Expressive theme, with its spring motion. The scheme is the tonal spot of the
 * app's seed (`#6A4F86`); [dynamicColor] (on by default, Android 12+) takes the wallpaper's colors
 * instead. [pureBlack] turns the dark theme's surfaces black. The theme (system, light, dark) is the
 * account's preference; the other two are the device's (docs/decisoes.md, D12).
 */
@Composable
fun PrumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colors = remember(darkTheme, dynamic, pureBlack) {
        val base = when {
            dynamic && darkTheme -> dynamicDarkColorScheme(context)
            dynamic -> dynamicLightColorScheme(context)
            else -> seedColorScheme(Seeds.APP, darkTheme)
        }
        if (darkTheme && pureBlack) base.pureBlack() else base
    }
    val extended = remember(colors, darkTheme) { ExtendedColors.of(colors.primary.toArgb(), darkTheme) }
    CompositionLocalProvider(
        LocalThemeSettings provides ThemeSettings(darkTheme, dynamic, pureBlack),
        LocalExtendedColors provides extended,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            shapes = PrumoShapes,
            typography = PrumoTypography,
            content = content,
        )
    }
}

/**
 * [content] in the colors of a module, a bank account or a category: the tonal-spot scheme of
 * [seed] (ARGB), nested in [PrumoTheme] and following its dark, pure black and dynamic color. With
 * dynamic color on, the seed is first harmonized with the system's primary, so the module keeps its
 * hue but sits well with the wallpaper.
 */
@Composable
fun ModuleTheme(seed: Int, content: @Composable () -> Unit) {
    val settings = LocalThemeSettings.current
    val appPrimary = MaterialTheme.colorScheme.primary
    val colors: ColorScheme = remember(seed, settings, appPrimary) {
        val effective = if (settings.dynamicColor) harmonize(seed, appPrimary.toArgb()) else seed
        val scheme = seedColorScheme(effective, settings.dark)
        if (settings.dark && settings.pureBlack) scheme.pureBlack() else scheme
    }
    val extended = remember(colors, settings.dark) { ExtendedColors.of(colors.primary.toArgb(), settings.dark) }
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MaterialTheme.motionScheme,
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}

/** What MaterialTheme doesn't have. */
object PrumoTheme {
    /** success and warning, harmonized with the current scheme. */
    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
