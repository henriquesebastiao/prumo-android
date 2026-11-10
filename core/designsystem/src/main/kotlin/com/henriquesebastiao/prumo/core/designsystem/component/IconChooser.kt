package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import java.text.Normalizer

/** One icon of an [IconChooser]: the [key] the app stores, its drawing and its name. */
data class IconChoice(val key: String, @param:DrawableRes val icon: Int, val label: String)

/** A titled group of an [IconChooser] ("Casa", "Transporte"). */
data class IconChoiceGroup(val title: String, val choices: List<IconChoice>)

/**
 * Every icon to choose from, in titled groups, with a search by name or group that ignores accents
 * ("saude" finds "Saúde"). The [selected] one is filled and square, as in [IconPicker].
 */
@Composable
fun IconChooser(
    groups: List<IconChoiceGroup>,
    selected: String?,
    onSelect: (String) -> Unit,
    searchLabel: String,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = filterIcons(groups, query)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(searchLabel) },
            leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (shown.isEmpty()) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        shown.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    group.choices.forEach { choice ->
                        IconChoiceButton(choice.icon, choice.label, choice.key == selected) { onSelect(choice.key) }
                    }
                }
            }
        }
    }
}

/** The groups and icons whose name or group contains [query], accents and case ignored. */
fun filterIcons(groups: List<IconChoiceGroup>, query: String): List<IconChoiceGroup> {
    val wanted = query.searchable()
    if (wanted.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val choices = if (group.title.searchable().contains(wanted)) {
            group.choices
        } else {
            group.choices.filter { it.label.searchable().contains(wanted) }
        }
        choices.takeIf { it.isNotEmpty() }?.let { group.copy(choices = it) }
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

private fun String.searchable(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

/** An icon to pick: filled and square when [selected], outlined otherwise; 48 dp. */
@Composable
internal fun IconChoiceButton(@DrawableRes icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val description = Modifier.semantics {
        contentDescription = label
        this.selected = selected
    }
    if (selected) {
        FilledIconButton(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = description.size(48.dp)) {
            Icon(painterResource(icon), contentDescription = null)
        }
    } else {
        OutlinedIconButton(onClick = onClick, modifier = description.size(48.dp)) {
            Icon(painterResource(icon), contentDescription = null)
        }
    }
}

@PhonePreviews
@Composable
private fun IconChooserPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            IconChooser(
                groups = listOf(
                    IconChoiceGroup(
                        "Casa",
                        listOf(
                            IconChoice("house", PrumoIcons.Home, "Casa"),
                            IconChoice("key", PrumoIcons.Key, "Aluguel"),
                            IconChoice("zap", PrumoIcons.Bolt, "Energia"),
                        ),
                    ),
                    IconChoiceGroup(
                        "Alimentação",
                        listOf(
                            IconChoice("utensils", PrumoIcons.Restaurant, "Alimentação"),
                            IconChoice("shopping-cart", PrumoIcons.ShoppingCart, "Mercado"),
                        ),
                    ),
                ),
                selected = "key",
                onSelect = {},
                searchLabel = "Buscar ícone",
                emptyText = "Nenhum ícone com esse nome.",
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
