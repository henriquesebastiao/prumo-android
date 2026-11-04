package com.henriquesebastiao.prumo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.color.argb
import com.henriquesebastiao.prumo.core.designsystem.color.extendedTones
import com.henriquesebastiao.prumo.core.designsystem.color.tonalScheme
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors

/** Material's tonal-spot color scheme of [seed] (ARGB), light or [dark]. */
fun seedColorScheme(seed: Int, dark: Boolean): ColorScheme {
    val scheme = tonalScheme(seed, dark)
    fun role(read: MaterialDynamicColors.() -> DynamicColor) = Color(scheme.argb(read))
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = role { primary },
        onPrimary = role { onPrimary },
        primaryContainer = role { primaryContainer },
        onPrimaryContainer = role { onPrimaryContainer },
        inversePrimary = role { inversePrimary },
        secondary = role { secondary },
        onSecondary = role { onSecondary },
        secondaryContainer = role { secondaryContainer },
        onSecondaryContainer = role { onSecondaryContainer },
        tertiary = role { tertiary },
        onTertiary = role { onTertiary },
        tertiaryContainer = role { tertiaryContainer },
        onTertiaryContainer = role { onTertiaryContainer },
        background = role { background },
        onBackground = role { onBackground },
        surface = role { surface },
        onSurface = role { onSurface },
        surfaceVariant = role { surfaceVariant },
        onSurfaceVariant = role { onSurfaceVariant },
        surfaceTint = role { surfaceTint },
        inverseSurface = role { inverseSurface },
        inverseOnSurface = role { inverseOnSurface },
        error = role { error },
        onError = role { onError },
        errorContainer = role { errorContainer },
        onErrorContainer = role { onErrorContainer },
        outline = role { outline },
        outlineVariant = role { outlineVariant },
        scrim = role { scrim },
        surfaceBright = role { surfaceBright },
        surfaceDim = role { surfaceDim },
        surfaceContainer = role { surfaceContainer },
        surfaceContainerHigh = role { surfaceContainerHigh },
        surfaceContainerHighest = role { surfaceContainerHighest },
        surfaceContainerLow = role { surfaceContainerLow },
        surfaceContainerLowest = role { surfaceContainerLowest },
        primaryFixed = role { primaryFixed },
        primaryFixedDim = role { primaryFixedDim },
        onPrimaryFixed = role { onPrimaryFixed },
        onPrimaryFixedVariant = role { onPrimaryFixedVariant },
        secondaryFixed = role { secondaryFixed },
        secondaryFixedDim = role { secondaryFixedDim },
        onSecondaryFixed = role { onSecondaryFixed },
        onSecondaryFixedVariant = role { onSecondaryFixedVariant },
        tertiaryFixed = role { tertiaryFixed },
        tertiaryFixedDim = role { tertiaryFixedDim },
        onTertiaryFixed = role { onTertiaryFixed },
        onTertiaryFixedVariant = role { onTertiaryFixedVariant },
    )
}

/**
 * [this] dark scheme in pure black, for OLED screens: background, surface, surfaceDim and the lowest
 * container black; the other containers darkened in Oklab (Low 0.6, Container 0.5, High 0.4,
 * Highest 0.3 of the way to black), so cards still stand out. As the design system's black theme.
 */
fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(surfaceContainerLow, Color.Black, BLACK_LOW),
    surfaceContainer = lerp(surfaceContainer, Color.Black, BLACK_CONTAINER),
    surfaceContainerHigh = lerp(surfaceContainerHigh, Color.Black, BLACK_HIGH),
    surfaceContainerHighest = lerp(surfaceContainerHighest, Color.Black, BLACK_HIGHEST),
)

private const val BLACK_LOW = 0.6f
private const val BLACK_CONTAINER = 0.5f
private const val BLACK_HIGH = 0.4f
private const val BLACK_HIGHEST = 0.3f

/** The roles Material's scheme lacks: success and warning, each in its four tones. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
) {
    companion object {
        /** success `#3B8A3E` and warning `#B5651D` harmonized with [seed] (ARGB). */
        fun of(seed: Int, dark: Boolean): ExtendedColors {
            val success = extendedTones(Seeds.SUCCESS, seed, dark)
            val warning = extendedTones(Seeds.WARNING, seed, dark)
            return ExtendedColors(
                success = success.color.asColor(),
                onSuccess = success.onColor.asColor(),
                successContainer = success.container.asColor(),
                onSuccessContainer = success.onContainer.asColor(),
                warning = warning.color.asColor(),
                onWarning = warning.onColor.asColor(),
                warningContainer = warning.container.asColor(),
                onWarningContainer = warning.onContainer.asColor(),
            )
        }

        private fun Int.asColor() = Color(this)
    }
}
