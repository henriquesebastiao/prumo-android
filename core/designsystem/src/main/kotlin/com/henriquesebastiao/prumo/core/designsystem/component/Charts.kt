package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** A slice of a [ShareBar]: its [amount] (any unit) and color. */
data class ShareSlice(val amount: Long, val color: Color)

/**
 * A stacked bar: each slice as wide as its share of the total, in its color, with a small gap
 * between them (expenses by category, board 02). The amounts are the server's; the bar only draws
 * them. [description] is what accessibility reads.
 */
@Composable
fun ShareBar(slices: List<ShareSlice>, description: String, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description },
    ) {
        val total = slices.sumOf { it.amount }.toFloat()
        if (total <= 0f) {
            drawRect(track)
            return@Canvas
        }
        val gap = 2.dp.toPx()
        var x = 0f
        slices.forEach { slice ->
            val width = size.width * slice.amount / total
            if (width > gap) drawRect(slice.color, Offset(x, 0f), Size(width - gap, size.height))
            x += width
        }
    }
}

/** One column of [PairedBars]: its label ("nov") and two values (income, expense). */
data class BarPair(val label: String, val first: Long, val second: Long)

/**
 * Two bars per column, side by side, scaled to the largest value (income × expense of the last six
 * months). The values are the server's; [description] is what accessibility reads.
 */
@Composable
fun PairedBars(
    columns: List<BarPair>,
    firstColor: Color,
    secondColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val max = columns.maxOfOrNull { maxOf(it.first, it.second) }?.takeIf { it > 0 } ?: 1L
    Column(modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / columns.size.coerceAtLeast(1)
            val bar = (slot * BAR_SHARE).coerceAtMost(14.dp.toPx())
            val radius = CornerRadius(bar / 2, bar / 2)
            columns.forEachIndexed { index, column ->
                val center = slot * index + slot / 2
                listOf(column.first to firstColor, column.second to secondColor).forEachIndexed {
                        side,
                        (value, color),
                    ->
                    val height = (size.height * value / max).coerceAtLeast(if (value > 0) 2.dp.toPx() else 0f)
                    val left = if (side == 0) center - bar - 1.dp.toPx() else center + 1.dp.toPx()
                    drawRoundRect(color, Offset(left, size.height - height), Size(bar, height), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
            columns.forEach {
                Text(
                    it.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A row of a chart's legend: the color, the name, a value and its share ("54 %"). */
@Composable
fun LegendRow(color: Color, label: String, value: String, share: String?, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = PrumoType.dataSmall)
        if (share != null) {
            Text(
                share,
                style = PrumoType.dataSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
    }
}

private const val BAR_SHARE = 0.3f

@PhonePreviews
@Composable
private fun ChartsPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                val violet = seededPrimary(0xFF6F57E8.toInt())
                val orange = seededPrimary(0xFFE4672E.toInt())
                val blue = seededPrimary(0xFF2F6FDE.toInt())
                ShareBar(listOf(ShareSlice(138_630, blue), ShareSlice(74_285, orange), ShareSlice(16_400, violet)), "")
                LegendRow(blue, "Moradia", "R$ 1.386,30", "54 %")
                LegendRow(orange, "Alimentação", "R$ 742,85", "29 %")
                PairedBars(
                    listOf(
                        BarPair("jun", 620_000, 280_000),
                        BarPair("jul", 640_000, 300_000),
                        BarPair("ago", 610_000, 350_000),
                        BarPair("set", 625_000, 410_000),
                        BarPair("out", 600_000, 260_000),
                        BarPair("nov", 565_000, 273_581),
                    ),
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.outline,
                    "",
                )
            }
        }
    }
}
