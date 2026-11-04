package com.henriquesebastiao.prumo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.R

// Fonts bundled in the APK (docs/decisoes.md, D06): Google Sans Flex, always in its rounded cut (the
// ROND axis at 100), and JetBrains Mono for values. Both are variable fonts; each weight is a
// variation of the same file.

private val Weights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

private const val ROUNDNESS = "ROND"
private const val NORMAL_WIDTH = 100f
private const val WIDE = 151f
private const val ROUNDED = 100f

/** Google Sans Flex, rounded, at its normal width: the whole interface. */
val GoogleSansFlex = FontFamily(
    Weights.map { weight ->
        Font(
            resId = R.font.google_sans_flex,
            weight = weight,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.width(NORMAL_WIDTH),
                FontVariation.Setting(ROUNDNESS, ROUNDED),
            ),
        )
    },
)

/** Google Sans Flex at its widest (wdth 151), for the hero title only. */
private val GoogleSansFlexWide = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.width(WIDE),
            FontVariation.Setting(ROUNDNESS, 0f),
        ),
    ),
)

/** JetBrains Mono: technical values and numbers that change (money, km, versions, times). */
val JetBrainsMono = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium).map { weight ->
        Font(
            resId = R.font.jetbrains_mono,
            weight = weight,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private val Base = Typography()

private fun TextStyle.role(weight: Int, size: Int, line: Int, tracking: Double = 0.0) = copy(
    fontFamily = GoogleSansFlex,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private fun TextStyle.emphasized(weight: Int) = copy(fontWeight = FontWeight(weight))

private val DisplayLarge = Base.displayLarge.role(700, 48, 56)
private val DisplayMedium = Base.displayMedium.role(700, 36, 44)
private val DisplaySmall = Base.displaySmall.role(400, 30, 38)
private val HeadlineLarge = Base.headlineLarge.role(600, 32, 40)
private val HeadlineMedium = Base.headlineMedium.role(600, 28, 36)
private val HeadlineSmall = Base.headlineSmall.role(600, 24, 32)
private val TitleLarge = Base.titleLarge.role(400, 22, 28)
private val TitleMedium = Base.titleMedium.role(500, 18, 24, 0.15)
private val TitleSmall = Base.titleSmall.role(500, 14, 20, 0.1)
private val BodyLarge = Base.bodyLarge.role(400, 16, 24, 0.5)
private val BodyMedium = Base.bodyMedium.role(400, 14, 20, 0.25)
private val BodySmall = Base.bodySmall.role(400, 12, 16, 0.4)
private val LabelLarge = Base.labelLarge.role(500, 16, 20, 0.1)
private val LabelMedium = Base.labelMedium.role(500, 14, 16, 0.5)
private val LabelSmall = Base.labelSmall.role(500, 11, 16, 0.5)

/**
 * The v3 scale of the design (docs/design-v1.md, "Tipografia"): bold 48/36 displays, semibold
 * headlines, 18 sp titleMedium, 16/14/11 sp labels. Each emphasized style is its role one weight up.
 */
val PrumoTypography = Typography(
    displayLarge = DisplayLarge,
    displayMedium = DisplayMedium,
    displaySmall = DisplaySmall,
    headlineLarge = HeadlineLarge,
    headlineMedium = HeadlineMedium,
    headlineSmall = HeadlineSmall,
    titleLarge = TitleLarge,
    titleMedium = TitleMedium,
    titleSmall = TitleSmall,
    bodyLarge = BodyLarge,
    bodyMedium = BodyMedium,
    bodySmall = BodySmall,
    labelLarge = LabelLarge,
    labelMedium = LabelMedium,
    labelSmall = LabelSmall,
    displayLargeEmphasized = DisplayLarge.emphasized(800),
    displayMediumEmphasized = DisplayMedium.emphasized(800),
    displaySmallEmphasized = DisplaySmall.emphasized(500),
    headlineLargeEmphasized = HeadlineLarge.emphasized(700),
    headlineMediumEmphasized = HeadlineMedium.emphasized(700),
    headlineSmallEmphasized = HeadlineSmall.emphasized(700),
    titleLargeEmphasized = TitleLarge.emphasized(500),
    titleMediumEmphasized = TitleMedium.emphasized(700),
    titleSmallEmphasized = TitleSmall.emphasized(700),
    bodyLargeEmphasized = BodyLarge.emphasized(500),
    bodyMediumEmphasized = BodyMedium.emphasized(500),
    bodySmallEmphasized = BodySmall.emphasized(500),
    labelLargeEmphasized = LabelLarge.emphasized(700),
    labelMediumEmphasized = LabelMedium.emphasized(700),
    labelSmallEmphasized = LabelSmall.emphasized(700),
)

/** The styles Material 3's scale doesn't have. Don't set a fontFamily in a screen: use these. */
object PrumoType {
    /** The wide hero title (welcome): 60/57 SemiBold, wdth 151, ROND 0, −0.02 em. */
    val hero: TextStyle = Base.displayLarge.copy(
        fontFamily = GoogleSansFlexWide,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        lineHeight = 0.95.em,
        letterSpacing = (-0.02).em,
    )

    /** [hero] in its small size (44/42): the Home's greeting. */
    val heroSmall: TextStyle = hero.copy(fontSize = 44.sp)

    /** Values in JetBrains Mono with tabular figures: 22/28 Medium (a balance on a card). */
    val dataLarge: TextStyle = data(FontWeight.Medium, 22, 28)

    /** 14/20 Medium: a value in a row or a tile. */
    val dataMedium: TextStyle = data(FontWeight.Medium, 14, 20)

    /** 12/16 Normal: a small value (a time, a version). */
    val dataSmall: TextStyle = data(FontWeight.Normal, 12, 16)

    /** The big balance of the Home's hero card: displayLarge's size with tabular figures. */
    val dataDisplay: TextStyle = DisplayLarge.copy(fontFeatureSettings = TABULAR)

    private fun data(weight: FontWeight, size: Int, line: Int) = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR,
    )

    private const val TABULAR = "tnum"
}
