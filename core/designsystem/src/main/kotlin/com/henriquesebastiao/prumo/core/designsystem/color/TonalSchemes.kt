package com.henriquesebastiao.prumo.core.designsystem.color

import com.materialkolor.blend.Blend
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.SchemeTonalSpot

// Plain Kotlin (no Compose, no Android): the colors of the design system from a seed, as ARGB.
// theme/ turns them into Compose color schemes.

/** The seeds of the design (docs/design-v1.md, "Cor"), as ARGB. */
object Seeds {
    /** The app's chrome: system screens, Home, Settings. */
    const val APP: Int = 0xFF6A4F86.toInt()
    const val FINANCE: Int = 0xFF2E6B4F.toInt()
    const val GARAGE: Int = 0xFF1F5F8B.toInt()
    const val READING: Int = 0xFF8A4B2E.toInt()
    const val STUDIES: Int = 0xFF1C6B74.toInt()
    const val SUCCESS: Int = 0xFF3B8A3E.toInt()
    const val WARNING: Int = 0xFFB5651D.toInt()
}

/**
 * Material's tonal-spot scheme (contrast 0, the 2021 spec the design was generated with) of
 * [seed], light or [dark]. Read its roles with [argb].
 */
fun tonalScheme(seed: Int, dark: Boolean): DynamicScheme = SchemeTonalSpot(
    Hct.fromInt(seed),
    dark,
    0.0,
    ColorSpec.SpecVersion.SPEC_2021,
    DynamicScheme.Platform.PHONE,
)

/** The roles of a scheme, as `scheme.argb { primary }`. */
val Roles = MaterialDynamicColors()

/** The ARGB of [role] in this scheme. */
inline fun DynamicScheme.argb(role: MaterialDynamicColors.() -> DynamicColor): Int = Roles.role().getArgb(this)

/** [color] with its hue moved a little towards [towards] (Material's harmonize), as the design does. */
fun harmonize(color: Int, towards: Int): Int = Blend.harmonize(color, towards)

/** A color role outside Material's scheme (success, warning), in its four tones. */
data class ToneSet(val color: Int, val onColor: Int, val container: Int, val onContainer: Int)

/**
 * [color] harmonized with [seed], in the tones of the design: 40/100/90/30 in light and 80/20/30/90
 * in [dark].
 */
fun extendedTones(color: Int, seed: Int, dark: Boolean): ToneSet {
    val palette = TonalPalette.fromInt(harmonize(color, seed))
    return if (dark) {
        ToneSet(palette.tone(80), palette.tone(20), palette.tone(30), palette.tone(90))
    } else {
        ToneSet(palette.tone(40), palette.tone(100), palette.tone(90), palette.tone(30))
    }
}
