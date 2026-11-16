package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.henriquesebastiao.prumo.core.designsystem.R
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * A grouped list whose rows are dragged by a handle into a new order (the modules of Settings, the
 * habits without a time). [row] builds each row and puts the [DragHandle] it receives where it
 * wants (usually in a [RowAction.Static]); [onReorder] gets the new order when a drag ends. The
 * handle also offers "Mover para cima/baixo" to accessibility services. No dependency: a vertical
 * drag moves the row one place each time it crosses half of the next one.
 */
@Composable
fun <K : Any> ReorderableColumn(
    keys: List<K>,
    onReorder: (List<K>) -> Unit,
    label: @Composable (K) -> String,
    modifier: Modifier = Modifier,
    style: ListStyle = ListStyle.Segmented,
    row: @Composable (key: K, handle: @Composable () -> Unit) -> GroupedRow,
) {
    var order by remember(keys) { mutableStateOf(keys) }
    var dragging by remember { mutableStateOf<K?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val moveUp = stringResource(R.string.designsystem_move_up)
    val moveDown = stringResource(R.string.designsystem_move_down)

    fun move(item: K, by: Int) {
        val from = order.indexOf(item)
        val to = from + by
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(style.gap)) {
        order.forEachIndexed { index, item ->
            // The key keeps each row (and its drag gesture) with its item while the order changes
            // under the finger; without it the gesture moved to another row and never ended.
            key(item) {
                val isDragging = dragging == item
                val name = label(item)
                val gesture = Modifier.pointerInput(item) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = item
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
                            move(item, 1)
                            offset -= step
                        } else if (offset < -step / 2) {
                            move(item, -1)
                            offset += step
                        }
                    }
                }
                val handle: @Composable () -> Unit = {
                    Icon(
                        painterResource(PrumoIcons.DragIndicator),
                        contentDescription = stringResource(R.string.designsystem_drag, name),
                        modifier = gesture.size(40.dp).padding(Spacing.sm),
                    )
                }
                GroupedListItem(
                    row(item, handle),
                    index = index,
                    count = order.size,
                    style = style,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .onSizeChanged { rowHeight = it.height }
                        .graphicsLayer { translationY = if (isDragging) offset else 0f }
                        .then(
                            if (isDragging) {
                                Modifier.shadow(DRAG_ELEVATION, style.shapeOf(index, order.size))
                            } else {
                                Modifier
                            },
                        )
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(moveUp) {
                                    move(item, -1)
                                    onReorder(order)
                                    true
                                },
                                CustomAccessibilityAction(moveDown) {
                                    move(item, 1)
                                    onReorder(order)
                                    true
                                },
                            )
                        },
                )
            }
        }
    }
}

private val DRAG_ELEVATION = 6.dp

@PhonePreviews
@Composable
private fun ReorderableColumnPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            ReorderableColumn(
                keys = listOf("Anki", "Beber água", "Tarefa do mês"),
                onReorder = {},
                label = { it },
                modifier = Modifier.padding(Spacing.screen),
            ) { name, handle ->
                GroupedRow(title = name, action = RowAction.Static(handle))
            }
        }
    }
}
