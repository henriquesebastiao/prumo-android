package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.R
import com.henriquesebastiao.prumo.core.designsystem.color.harmonize
import com.henriquesebastiao.prumo.core.designsystem.theme.LocalThemeSettings
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.designsystem.theme.seedColorScheme

/**
 * "Novembro 2026" between the previous and next buttons (the month of Finanças, a card's invoices).
 * A null callback disables its button.
 */
@Composable
fun PeriodSelector(
    label: String,
    previousLabel: String,
    nextLabel: String,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onPrevious?.invoke() },
            enabled = onPrevious != null,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(painterResource(PrumoIcons.ChevronLeft), contentDescription = previousLabel)
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            trailing?.invoke()
        }
        IconButton(
            onClick = { onNext?.invoke() },
            enabled = onNext != null,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(painterResource(PrumoIcons.ChevronRight), contentDescription = nextLabel)
        }
    }
}

/**
 * The primary color of the tonal scheme of [seed], as [ModuleTheme] draws it (harmonized with the
 * dynamic color when it's on): a category's slice of a chart, a color swatch.
 */
@Composable
fun seededPrimary(seed: Int): Color {
    val settings = LocalThemeSettings.current
    val appPrimary = MaterialTheme.colorScheme.primary
    return remember(seed, settings.dark, settings.dynamicColor, appPrimary) {
        val effective = if (settings.dynamicColor) harmonize(seed, appPrimary.toArgb()) else seed
        seedColorScheme(effective, settings.dark).primary
    }
}

/** An icon in the avatar of its own color (an account, a category): the tonal scheme of [seed]. */
@Composable
fun SeededAvatar(
    seed: Int,
    icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    cookie: Boolean = false,
) {
    ModuleTheme(seed) {
        IconAvatar(
            icon,
            modifier = modifier,
            size = size,
            iconSize = iconSize,
            shape = if (cookie) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
        )
    }
}

/** One choice of a [ColorPicker]: its [seed] and the name read by accessibility. */
data class ColorOption(val seed: Int, val label: String)

/** Round swatches, one per color; the selected one has a check and a ring. */
@Composable
fun ColorPicker(options: List<ColorOption>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // The design system's color picker: 36 dp circles in 48 dp touch areas, side by side; the
    // selected one gets a 2 dp ring in onSurface and a check.
    FlowRow(
        modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val color = seededPrimary(option.seed)
            val checked = index == selected
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = option.label }
                    .selectable(selected = checked, role = Role.RadioButton) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(if (checked) 42.dp else 36.dp)
                        .then(
                            if (checked) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .padding(if (checked) 4.dp else 0.dp)
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            painterResource(PrumoIcons.Check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One choice of an [IconPicker]. */
data class IconOption(val icon: Int, val label: String)

/** Icons to choose from: the selected one filled and square, the others outlined. */
@Composable
fun IconPicker(options: List<IconOption>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        options.forEachIndexed { index, option ->
            val description = Modifier.semantics { contentDescription = option.label }
            if (index == selected) {
                FilledIconButton(
                    onClick = { onSelect(index) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = description.size(48.dp),
                ) {
                    Icon(painterResource(option.icon), contentDescription = null)
                }
            } else {
                OutlinedIconButton(onClick = { onSelect(index) }, modifier = description.size(48.dp)) {
                    Icon(painterResource(option.icon), contentDescription = null)
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun PickersPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                PeriodSelector(
                    "Novembro 2026",
                    stringResource(R.string.designsystem_previous),
                    stringResource(R.string.designsystem_next),
                    onPrevious = {},
                    onNext = null,
                )
                SeededAvatar(0xFFE4672E.toInt(), PrumoIcons.Restaurant, cookie = true, size = 56.dp)
                ColorPicker(
                    listOf(
                        ColorOption(0xFF2F6FDE.toInt(), "Azul"),
                        ColorOption(0xFF6F57E8.toInt(), "Violeta"),
                        ColorOption(0xFFE4672E.toInt(), "Laranja"),
                        ColorOption(0xFF24A06B.toInt(), "Verde"),
                    ),
                    selected = 2,
                    onSelect = {},
                )
                IconPicker(
                    listOf(
                        IconOption(PrumoIcons.Restaurant, "Alimentação"),
                        IconOption(PrumoIcons.Home, "Casa"),
                        IconOption(PrumoIcons.DirectionsBus, "Transporte"),
                    ),
                    selected = 0,
                    onSelect = {},
                )
            }
        }
    }
}
