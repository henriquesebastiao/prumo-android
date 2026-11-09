package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoShape
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** A [SectionCard]'s container: a surface step, or a container tone for a highlight. */
enum class SectionTone { Low, Container, High, Highest, Primary, Secondary, Tertiary }

/**
 * A tonal card that groups one subject: no shadow, no border, depth from its tone. [hero] makes it
 * the screen's focal point (32 dp corners, 20 dp padding, a larger avatar and title); one per
 * screen. The header (icon, overline, title, trailing) is optional; [content] goes below it.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    tone: SectionTone = SectionTone.Container,
    hero: Boolean = false,
    icon: Int? = null,
    overline: String? = null,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val (container, onContainer) = tone.colors()
    val shape = if (hero) PrumoShape.Hero else PrumoShape.Section
    val hasHeader = listOf(icon, overline, title, trailing).any { it != null }
    val body: @Composable () -> Unit = {
        Column(
            Modifier.padding(if (hero) HERO_PADDING else Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (hasHeader) SectionHeaderRow(icon, overline, title, trailing, hero, tinted = onContainer != null)
            content()
        }
    }
    val contentColor = onContainer ?: MaterialTheme.colorScheme.onSurface
    if (onClick == null) {
        Surface(modifier, shape = shape, color = container, contentColor = contentColor, content = body)
    } else {
        Surface(onClick, modifier, shape = shape, color = container, contentColor = contentColor, content = body)
    }
}

@Composable
private fun SectionHeaderRow(
    icon: Int?,
    overline: String?,
    title: String?,
    trailing: (@Composable () -> Unit)?,
    hero: Boolean,
    tinted: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val content = LocalContentColor.current
    val muted = if (tinted) content.copy(alpha = MUTED_ALPHA) else scheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (icon != null) {
            IconAvatar(
                icon,
                container = if (tinted) content.copy(alpha = AVATAR_ALPHA) else scheme.primaryContainer,
                content = if (tinted) content else scheme.onPrimaryContainer,
                size = if (hero) 56.dp else 40.dp,
                shape = if (hero) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
            )
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    overline,
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (title != null) {
                Text(
                    title,
                    style = if (hero) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        trailing?.invoke()
    }
}

/** An icon in a tonal [shape] (a circle, or a Material shape such as `MaterialShapes.Cookie9Sided`). */
@Composable
fun IconAvatar(
    icon: Int,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape,
) {
    Box(modifier.size(size).background(container, shape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
    }
}

/** A [SectionCard]'s container and its content color, when the tone has one of its own. */
@Composable
private fun SectionTone.colors(): Pair<Color, Color?> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        SectionTone.Low -> scheme.surfaceContainerLow to null
        SectionTone.Container -> scheme.surfaceContainer to null
        SectionTone.High -> scheme.surfaceContainerHigh to null
        SectionTone.Highest -> scheme.surfaceContainerHighest to null
        SectionTone.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        SectionTone.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
        SectionTone.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}

/**
 * A labeled value on its own surface, inside a card: "Gastos de novembro" over "R$ 1.482,90",
 * "Versão" over "Prumo 1.0.2". The value is in the data face and steps down instead of breaking.
 */
@Composable
fun KpiTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = PrumoType.dataMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    valueColor: Color = Color.Unspecified,
) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                style = valueStyle,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 11.sp,
                    maxFontSize = valueStyle.fontSize,
                    stepSize = 1.sp,
                ),
            )
        }
    }
}

private val HERO_PADDING = 20.dp
private const val MUTED_ALPHA = 0.78f
private const val AVATAR_ALPHA = 0.14f

@PhonePreviews
@Composable
private fun ContainersPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                SectionCard(
                    tone = SectionTone.Primary,
                    hero = true,
                    icon = PrumoIcons.AccountBalanceWallet,
                    overline = "Finanças",
                    title = "Saldo total",
                ) {
                    Text("R$ 8.742,35", style = PrumoType.dataDisplay)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        KpiTile("R$ 1.482,90", "Gastos de novembro", Modifier.weight(1f))
                        KpiTile("R$ 1.064,22", "Fatura Nubank", Modifier.weight(1f))
                    }
                }
                SectionCard(icon = PrumoIcons.Dns, title = "prumo.minhacasa.net", onClick = {})
            }
        }
    }
}
