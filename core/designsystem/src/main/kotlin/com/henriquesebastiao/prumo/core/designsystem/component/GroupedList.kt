package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** What tapping a [GroupedRow] does, and what it shows at the end. */
sealed interface RowAction {
    /**
     * Opens something; [chevron] says it's a screen. A [trailing] value (a total, a date) takes the
     * chevron's place.
     */
    data class Click(
        val onClick: () -> Unit,
        val chevron: Boolean = true,
        val trailing: (@Composable () -> Unit)? = null,
    ) : RowAction

    /** A switch: the whole row toggles it. */
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : RowAction

    /** Nothing to tap; [trailing] is drawn at the end (a button, a drag handle and a switch). */
    data class Static(val trailing: (@Composable () -> Unit)? = null) : RowAction
}

/**
 * One row of a [GroupedList]. The [leading] slot takes the place of the icon avatar (a module's
 * avatar in its own colors). A [destructive] row ("Sair") is in the error colors only when asked.
 */
data class GroupedRow(
    val title: String,
    val action: RowAction,
    val summary: String? = null,
    val icon: Int? = null,
    val enabled: Boolean = true,
    val emphasized: Boolean = false,
    val neutral: Boolean = false,
    val leading: (@Composable () -> Unit)? = null,
)

/**
 * A group of rows as Material 3 Expressive draws a grouped list (`SegmentedListItem`): each row its
 * own surface, the ends of the group with large corners, a small gap between rows ([style]). A
 * row's icon sits in a primary container circle, or a neutral one with [GroupedRow.neutral] (Sair,
 * the archived items); [GroupedRow.emphasized] makes the row larger (the account at the top of
 * Settings).
 */
@Composable
fun GroupedList(rows: List<GroupedRow>, modifier: Modifier = Modifier, style: ListStyle = ListStyle.Segmented) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(style.gap)) {
        rows.forEachIndexed { index, row -> GroupedListItem(row, index, rows.size, style = style) }
    }
}

/** A row that is the [index]th of [count] in its group, which decides its corners. */
@Composable
fun GroupedListItem(
    row: GroupedRow,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    style: ListStyle = ListStyle.Segmented,
) {
    val shape = style.shapeOf(index, count)
    val shapes = ListItemShapes(shape, shape, shape, shape, shape, shape)
    val scheme = MaterialTheme.colorScheme
    val colors = ListItemDefaults.segmentedColors(
        containerColor = scheme.surfaceContainer,
        selectedContainerColor = scheme.surfaceContainer,
        selectedContentColor = scheme.onSurface,
        selectedLeadingContentColor = scheme.onSurfaceVariant,
        selectedTrailingContentColor = scheme.onSurfaceVariant,
        selectedSupportingContentColor = scheme.onSurfaceVariant,
    )
    val leading: @Composable (() -> Unit)? = row.leading ?: row.icon?.let { icon ->
        {
            IconAvatar(
                icon,
                container = if (row.neutral) scheme.surfaceContainerHighest else scheme.primaryContainer,
                content = if (row.neutral) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                modifier = if (row.enabled) Modifier else Modifier.alpha(DISABLED_ALPHA),
            )
        }
    }
    val supporting: @Composable (() -> Unit)? = row.summary?.let { summary -> { Text(summary) } }
    val title: @Composable () -> Unit = {
        Text(
            row.title,
            style = if (row.emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = Color.Unspecified,
        )
    }
    when (val action = row.action) {
        is RowAction.Click -> SegmentedListItem(
            onClick = action.onClick,
            shapes = shapes,
            modifier = modifier,
            enabled = row.enabled,
            leadingContent = leading,
            trailingContent = action.trailing ?: if (action.chevron) {
                {
                    Icon(
                        painterResource(PrumoIcons.ChevronRight),
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            supportingContent = supporting,
            colors = colors,
            content = title,
        )

        is RowAction.Toggle -> SegmentedListItem(
            checked = action.checked,
            onCheckedChange = action.onCheckedChange,
            shapes = shapes,
            modifier = modifier,
            enabled = row.enabled,
            leadingContent = leading,
            trailingContent = { Switch(checked = action.checked, onCheckedChange = null, enabled = row.enabled) },
            supportingContent = supporting,
            colors = colors,
            content = title,
        )

        is RowAction.Static -> SegmentedListItem(
            shapes = shapes,
            modifier = modifier,
            enabled = row.enabled,
            leadingContent = leading,
            trailingContent = action.trailing,
            supportingContent = supporting,
            colors = colors,
            content = title,
        )
    }
}

/** A short note under a group ("Trocar a senha encerra as outras sessões."). */
@Composable
fun GroupNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

private const val DISABLED_ALPHA = 0.38f

@PhonePreviews
@Composable
private fun GroupedListPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                GroupedList(
                    listOf(
                        GroupedRow(
                            "Marina Costa",
                            RowAction.Click({}),
                            "marina · nome e senha",
                            PrumoIcons.Person,
                            emphasized = true,
                        ),
                    ),
                )
                GroupedList(
                    listOf(
                        GroupedRow(
                            "Aparência",
                            RowAction.Click({
                            }),
                            "Tema do sistema · cor dinâmica",
                            PrumoIcons.Palette,
                        ),
                        GroupedRow(
                            "Preto puro",
                            RowAction.Toggle(true) {
                            },
                            "Fundo preto para telas OLED.",
                            PrumoIcons.Contrast,
                        ),
                        GroupedRow("Sair", RowAction.Click({}, chevron = false), "Encerra a sessão", PrumoIcons.Logout),
                    ),
                )
                GroupNote("O tamanho do texto segue a configuração de fonte do Android.")
            }
        }
    }
}
