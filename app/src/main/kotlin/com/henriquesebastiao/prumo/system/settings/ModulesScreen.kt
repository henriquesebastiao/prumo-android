package com.henriquesebastiao.prumo.system.settings

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.ui.PreviewData

@Composable
fun ModulesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ModulesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModulesScreen(state, viewModel::setEnabled, viewModel::reorder, viewModel::load, onBack, modifier)
}

/**
 * Settings › Módulos: each module with its switch and a handle to drag it; the order is the Home's
 * and "Todas as ações"'s. Disabling hides the module in the app; its data stays on the server.
 * The handle also offers "Mover para cima/baixo" to accessibility services.
 */
@Composable
fun ModulesScreen(
    state: ModulesUiState,
    onEnabled: (String, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(R.string.modules_title), onBack) }) { padding ->
        when {
            state.loading && state.items.isEmpty() -> LoadingState(
                stringResource(R.string.modules_title),
                Modifier.padding(padding),
            )

            state.error != null && state.items.isEmpty() -> ErrorState(state.error, onRetry, Modifier.padding(padding))

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    stringResource(R.string.modules_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReorderableModules(state.items, onEnabled, onReorder)
                if (state.unknown > 0) GroupNote(stringResource(R.string.modules_not_in_app))
            }
        }
    }
}

@Composable
private fun ReorderableModules(
    items: List<ModuleItem>,
    onEnabled: (String, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    var order by remember(items) { mutableStateOf(items.map { it.id }) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val byId = items.associateBy { it.id }
    val style = ListStyle.Segmented
    val moveUp = stringResource(R.string.modules_move_up)
    val moveDown = stringResource(R.string.modules_move_down)

    fun move(id: String, by: Int) {
        val from = order.indexOf(id)
        val to = from + by
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(style.gap)) {
        order.forEachIndexed { index, id ->
            val item = byId[id] ?: return@forEachIndexed
            val isDragging = dragging == id
            val name = stringResource(item.feature.nameRes)
            val handle = Modifier.pointerInput(id) {
                detectDragGestures(
                    onDragStart = {
                        dragging = id
                        offset = 0f
                    },
                    onDragEnd = {
                        dragging = null
                        offset = 0f
                        onReorder(order)
                    },
                    onDragCancel = {
                        dragging = null
                        offset = 0f
                    },
                ) { change, amount ->
                    change.consume()
                    offset += amount.y
                    val step = rowHeight + style.gap.toPx()
                    if (offset > step / 2) {
                        move(id, 1)
                        offset -= step
                    } else if (offset < -step / 2) {
                        move(id, -1)
                        offset += step
                    }
                }
            }
            GroupedListItem(
                GroupedRow(
                    title = name,
                    action = RowAction.Static {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Switch(checked = item.enabled, onCheckedChange = { onEnabled(id, it) })
                            Icon(
                                painterResource(PrumoIcons.DragIndicator),
                                contentDescription = stringResource(R.string.modules_drag, name),
                                modifier = handle.size(40.dp).padding(Spacing.sm),
                            )
                        }
                    },
                    summary = stringResource(
                        if (item.enabled) item.feature.descriptionRes else R.string.modules_disabled,
                    ),
                    leading = {
                        ModuleTheme(item.feature.seed) { IconAvatar(item.feature.icon) }
                    },
                ),
                index = index,
                count = order.size,
                style = style,
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .onSizeChanged { rowHeight = it.height }
                    .graphicsLayer { translationY = if (isDragging) offset else 0f }
                    .then(
                        if (isDragging) Modifier.shadow(DRAG_ELEVATION, style.shapeOf(index, order.size)) else Modifier,
                    )
                    .semantics {
                        contentDescription = name
                        customActions = listOf(
                            CustomAccessibilityAction(moveUp) {
                                move(id, -1)
                                onReorder(order)
                                true
                            },
                            CustomAccessibilityAction(moveDown) {
                                move(id, 1)
                                onReorder(order)
                                true
                            },
                        )
                    },
            )
        }
    }
}

private val DRAG_ELEVATION = 6.dp

@PhonePreviews
@Composable
private fun ModulesPreview() {
    PrumoTheme(dynamicColor = false) {
        ModulesScreen(PreviewData.modules, onEnabled = { _, _ -> }, onReorder = {}, onRetry = {}, onBack = {})
    }
}
