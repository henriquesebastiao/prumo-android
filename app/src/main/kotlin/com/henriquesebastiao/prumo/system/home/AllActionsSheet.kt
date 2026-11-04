package com.henriquesebastiao.prumo.system.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.ActionGrid
import com.henriquesebastiao.prumo.core.designsystem.component.GridAction
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.QuickAction

/**
 * Every quick action of the enabled modules, grouped by module in the user's order, each group in
 * its module's colors. "Editar" chooses the four pinned to the Home's toolbar (kept on the device).
 */
@Composable
fun AllActionsSheet(
    groups: List<ActionGroup>,
    pinned: Set<String>,
    onAction: (QuickAction) -> Unit,
    onTogglePinned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
                .navigationBarsPadding()
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_all_actions),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = { editing = !editing }) {
                    if (!editing) Icon(painterResource(PrumoIcons.Edit), contentDescription = null)
                    Text(
                        stringResource(if (editing) R.string.home_done_editing else R.string.home_edit_actions),
                        Modifier.padding(start = Spacing.xs),
                    )
                }
            }
            if (editing) {
                Text(
                    stringResource(R.string.home_edit_actions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            groups.forEach { group ->
                ModuleTheme(group.feature.seed) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionHeader(stringResource(group.feature.nameRes))
                        ActionGrid(
                            group.actions.map { action ->
                                GridAction(
                                    icon = action.icon,
                                    label = stringResource(action.shortLabelRes),
                                    selected = if (editing) action.id in pinned else null,
                                ) {
                                    if (editing) onTogglePinned(action.id) else onAction(action)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
