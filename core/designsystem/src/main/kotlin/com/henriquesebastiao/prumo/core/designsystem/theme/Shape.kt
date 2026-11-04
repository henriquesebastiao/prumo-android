package com.henriquesebastiao.prumo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Material 3 Expressive corner scale: extraSmall 4 · small 8 · medium 12 · large 16 ·
 * largeIncreased 20 · extraLarge 28 · extraLargeIncreased 32 · extraExtraLarge 48.
 */
val PrumoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

/** Shapes the M3 scale doesn't name, by what they're for. */
object PrumoShape {
    /** A section card (a module's card on the Home, a settings group). */
    val Section = RoundedCornerShape(24.dp)

    /** The screen's one hero card. */
    val Hero = RoundedCornerShape(32.dp)

    /** Sheets and dialogs. */
    val Sheet = RoundedCornerShape(28.dp)

    /** The footer of the first run: rounded on top, flush with the screen's bottom. */
    val OnboardingFooter = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}

/**
 * How a list groups its items: [outer] corners at the ends of the group, [inner] corners between
 * items, and the [gap] between them.
 */
@Immutable
data class ListStyle(val outer: Dp, val inner: Dp, val gap: Dp) {
    /** The corners of the [index]th item of [count]. */
    fun shapeOf(index: Int, count: Int): RoundedCornerShape {
        val top = if (index == 0) outer else inner
        val bottom = if (index == count - 1) outer else inner
        return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }

    companion object {
        /** Settings and menus: 24 at the ends, 8 between, 4 apart. */
        val Segmented = ListStyle(outer = 24.dp, inner = 8.dp, gap = 4.dp)

        /** Content (records of a module): every item a 24 dp card, 8 apart. */
        val Separated = ListStyle(outer = 24.dp, inner = 24.dp, gap = 8.dp)
    }
}

/** Spacing on the 4 dp grid. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Screen margins (the gutter). */
    val screen = 16.dp

    /** Between sections of a screen. */
    val section = 24.dp
}
