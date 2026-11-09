package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/** One labeled action of an [ActionGrid]. [selected] non-null makes it a toggle (choosing pinned actions). */
data class GridAction(val icon: Int, val label: String, val selected: Boolean? = null, val onClick: () -> Unit)

/**
 * Labeled actions in rows of [columns]: a square tonal icon button over its label, as "Todas as
 * ações" of the Home draws them. Each button is announced by its label.
 */
@Composable
fun ActionGrid(actions: List<GridAction>, modifier: Modifier = Modifier, columns: Int = 4) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        actions.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { action -> ActionCell(action, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActionCell(action: GridAction, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val selected = action.selected
        val scheme = MaterialTheme.colorScheme
        FilledTonalIconButton(
            onClick = action.onClick,
            shapes = IconButtonDefaults.shapes(
                shape = if (selected ==
                    true
                ) {
                    CircleShape
                } else {
                    MaterialTheme.shapes.large
                },
            ),
            colors = if (selected == true) {
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                )
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            },
            modifier = Modifier.size(CELL_SIZE).semantics {
                contentDescription = action.label
                if (selected != null) {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(selected)
                }
            },
        ) {
            Icon(painterResource(if (selected == true) PrumoIcons.Check else action.icon), contentDescription = null)
        }
        Box(Modifier.clearAndSetSemantics {}) {
            // Words never break in the middle: a single long word ("Abastecimento") runs a little
            // into the gap between cells, as in the design; several words wrap between them.
            val oneWord = action.label.none { it.isWhitespace() }
            Text(
                action.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = if (oneWord) 1 else 2,
                softWrap = !oneWord,
                modifier = if (oneWord) Modifier.wrapContentWidth(unbounded = true) else Modifier,
            )
        }
    }
}

private val CELL_SIZE = 56.dp

@PhonePreviews
@Composable
private fun ActionGridPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            ActionGrid(
                listOf(
                    GridAction(PrumoIcons.Payments, "Despesa") {},
                    GridAction(PrumoIcons.Savings, "Receita", selected = true) {},
                    GridAction(PrumoIcons.SwapHoriz, "Transferência") {},
                    GridAction(PrumoIcons.CreditCard, "Pagar fatura") {},
                    GridAction(PrumoIcons.LocalGasStation, "Abastecimento") {},
                ),
                Modifier.padding(Spacing.lg),
            )
        }
    }
}
