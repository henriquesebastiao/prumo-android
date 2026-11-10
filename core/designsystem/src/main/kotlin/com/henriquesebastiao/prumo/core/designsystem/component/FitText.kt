package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * One line that never wraps and never cuts a character: when [text] doesn't fit (a big balance, a
 * large font in the Android settings, a 360 dp screen), its size steps down from the [style]'s to
 * [minFontSize]. Only below that does it end in "…". Values (money, km, percentages) and titles use
 * it; a value is never broken across lines nor shown with a digit missing.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minFontSize: TextUnit = MIN_FONT_SIZE,
) {
    val max = style.fontSize.takeIf { it.isSp } ?: LocalTextStyle.current.fontSize
    Text(
        text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = if (max.isSp && max.value > minFontSize.value) {
            TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = max, stepSize = STEP)
        } else {
            null
        },
    )
}

/**
 * The value at the end of a list row (a balance, an amount): at most [ROW_VALUE_MAX] wide, so a big
 * value shrinks instead of squeezing the row's title and supporting text into several lines.
 */
@Composable
fun RowValue(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    FitText(
        text,
        modifier.widthIn(max = ROW_VALUE_MAX),
        style = PrumoType.dataSmall,
        color = color,
        textAlign = TextAlign.End,
    )
}

private val ROW_VALUE_MAX = 96.dp

/** Small enough for a long balance at the largest font, still legible. */
private val MIN_FONT_SIZE = 11.sp
private val STEP = 1.sp

@PhonePreviews
@Composable
private fun FitTextPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FitText("R$ 7.154,27", style = PrumoType.dataDisplay)
                FitText("R$ 1.237.154,27", Modifier.width(200.dp), style = PrumoType.dataDisplay)
                FitText("Configurações", Modifier.width(220.dp), style = PrumoType.hero)
            }
        }
    }
}
