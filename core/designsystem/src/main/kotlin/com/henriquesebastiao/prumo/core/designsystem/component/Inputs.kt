package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * A field of a number typed as digits only (money in cents, km): it keeps what was typed and
 * [transformation] draws it ("4636" as "R$ 46,36", "18512" as "18.512"). A key that isn't a digit,
 * or one digit too many ([maxDigits]), is ignored: the text is never rewritten (CLAUDE.md, "Campos
 * de texto"). [large] is the big value of a sheet ("Valor total").
 */
@Composable
fun ValueField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    transformation: VisualTransformation,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    leadingIcon: Int? = null,
    suffix: String? = null,
    supportingText: String? = null,
    error: String? = null,
    maxDigits: Int = MAX_DIGITS,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = digits,
        suffix = suffix?.let { { Text(it) } },
        onValueChange = { text -> if (text.length <= maxDigits && text.all(Char::isDigit)) onDigitsChange(text) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        textStyle = if (large) PrumoType.dataLarge else PrumoType.dataMedium,
        leadingIcon = leadingIcon?.let { icon -> { Icon(painterResource(icon), contentDescription = null) } },
        supportingText = (error ?: supportingText)?.let { text -> { Text(text) } },
        isError = error != null,
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        singleLine = true,
    )
}

/**
 * A decimal number with a comma ("7,3" litres), up to [scale] decimals. A point from the keyboard
 * counts as the comma; anything else is ignored.
 */
@Composable
fun DecimalField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    scale: Int,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    leadingIcon: Int? = null,
    supportingText: String? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = text,
        onValueChange = { new -> if (isDecimal(new, scale)) onTextChange(new) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        textStyle = PrumoType.dataMedium,
        leadingIcon = leadingIcon?.let { icon -> { Icon(painterResource(icon), contentDescription = null) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = (error ?: supportingText)?.let { message -> { Text(message) } },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        singleLine = true,
    )
}

private fun isDecimal(text: String, scale: Int): Boolean {
    val parts = text.split(',', '.')
    return parts.size <= 2 && parts.all { part -> part.all(Char::isDigit) } &&
        parts[0].length <= MAX_DIGITS - scale && (parts.size == 1 || parts[1].length <= scale)
}

/** One suggestion of an [AutocompleteField]: [label] and, under it, [detail] ("3 manutenções"). */
data class Suggestion(val label: String, val detail: String? = null, val icon: Int? = null)

/**
 * A text field that suggests values as it is typed (the workshop of a maintenance, the oil of an
 * oil change): picking one calls [onPick] with its index. [createLabel] adds a last option to use
 * the typed text as a new value ("Criar oficina “Rocha”"), which calls [onCreate].
 */
@Composable
fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<Suggestion>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    createLabel: String? = null,
    onCreate: () -> Unit = {},
    supportingText: String? = null,
    error: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasOptions = suggestions.isNotEmpty() || createLabel != null
    ExposedDropdownMenuBox(expanded = expanded && hasOptions, onExpandedChange = {
        expanded = it
    }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            leadingIcon = leadingIcon?.let { icon -> { Icon(painterResource(icon), contentDescription = null) } },
            supportingText = (error ?: supportingText)?.let { message -> { Text(message) } },
            isError = error != null,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Next),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded && hasOptions, onDismissRequest = { expanded = false }) {
            suggestions.forEachIndexed { index, suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(suggestion.label)
                            suggestion.detail?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        onPick(index)
                        expanded = false
                    },
                    leadingIcon = suggestion.icon?.let { icon ->
                        { Icon(painterResource(icon), contentDescription = null) }
                    },
                )
            }
            if (createLabel != null) {
                DropdownMenuItem(
                    text = { Text(createLabel, color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onCreate()
                        expanded = false
                    },
                    leadingIcon = { Icon(painterResource(PrumoIcons.Add), contentDescription = null) },
                )
            }
        }
    }
}

/**
 * Tabs drawn as pills that scroll sideways (the history of a vehicle: Manutenções, Abastecimentos,
 * Óleo, Km): the selected one filled.
 */
@Composable
fun <T> PillTabs(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        options.forEach { option ->
            val checked = option == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(option) },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Tab },
            ) {
                Text(label(option), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** A value with its label in a big style ("Total calculado R$ 53,90"), for a form's preview. */
@Composable
fun PreviewValue(label: String, value: String, modifier: Modifier = Modifier, style: TextStyle = PrumoType.dataMedium) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = style)
    }
}

private const val MAX_DIGITS = 10

@PhonePreviews
@Composable
private fun InputsPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PillTabs(listOf("Manutenções", "Abastecimentos", "Óleo", "Km"), "Manutenções", { it }, {})
                ValueField("4636", {}, "Valor total", VisualTransformation.None, large = true)
                DecimalField("7,3", {}, "Litros", scale = 3, suffix = "L")
                AutocompleteField(
                    "Moto",
                    {},
                    "Oficina",
                    listOf(Suggestion("Moto Peças Rocha", "3 manutenções")),
                    {},
                    leadingIcon = PrumoIcons.Storefront,
                    createLabel = "Criar oficina “Moto”",
                )
                PreviewValue("Total calculado", "R$ 53,90")
            }
        }
    }
}
