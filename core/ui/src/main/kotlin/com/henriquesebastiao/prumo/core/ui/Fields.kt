package com.henriquesebastiao.prumo.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Draws the digits of a money field in [currency]: "4636" as "R$ 46,36". The cursor stays at the end. */
data class MoneyTransformation(val currency: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = Inputs.number(text.text)?.let { Formats.money(it, currency) }.orEmpty()
        return TransformedText(AnnotatedString(shown), EndOffsetMapping(text.length, shown.length))
    }
}

/** Draws the digits of a whole number with thousands separators: "18512" as "18.512". */
data object IntegerTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = Inputs.number(text.text)?.let(Formats::integer).orEmpty()
        return TransformedText(AnnotatedString(shown), EndOffsetMapping(text.length, shown.length))
    }
}

/** Every position maps to the end: typing and deleting happen at the end, as on a calculator. */
private class EndOffsetMapping(private val original: Int, private val transformed: Int) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = transformed

    override fun transformedToOriginal(offset: Int): Int = original
}

/**
 * When something happened, as a field that opens the date picker and then the time picker. [today]
 * (the user's, in their zone) shows as "Hoje, 14:30". [asTag] draws it as a tag (a quick sheet's
 * default that changes with a tap).
 */
@Composable
fun DateTimeField(
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit,
    label: String,
    today: LocalDate,
    modifier: Modifier = Modifier,
    asTag: Boolean = false,
) {
    var step by rememberSaveable { mutableStateOf(PickerStep.Closed) }
    var pickedDate by rememberSaveable { mutableStateOf(value.toLocalDate()) }
    val text = when (value.toLocalDate()) {
        today -> stringResource(R.string.ui_today_at, Formats.time(value.toLocalTime()))

        else -> stringResource(
            R.string.ui_date_at,
            Formats.date(value.toLocalDate()),
            Formats.time(value.toLocalTime()),
        )
    }
    if (asTag) {
        Tag(
            text,
            modifier.semantics { contentDescription = "$label: $text" },
            icon = PrumoIcons.Event,
            onClick = { step = PickerStep.Date },
        )
    } else {
        ClickableField(text, label, PrumoIcons.CalendarMonth, onClick = { step = PickerStep.Date }, modifier = modifier)
    }
    when (step) {
        PickerStep.Date -> DateDialog(
            initial = value.toLocalDate(),
            onPick = { date ->
                pickedDate = date
                step = PickerStep.Time
            },
            onDismiss = { step = PickerStep.Closed },
        )

        PickerStep.Time -> TimeDialog(
            initial = value.toLocalTime(),
            onPick = { time ->
                onValueChange(LocalDateTime.of(pickedDate, time))
                step = PickerStep.Closed
            },
            onDismiss = { step = PickerStep.Closed },
        )

        PickerStep.Closed -> Unit
    }
}

/**
 * A day (the next oil change's date), or none when [onClear] is given and used. [today] (the
 * user's) shows as "Hoje". [asTag] draws it as a tag (a quick sheet's default that changes with a
 * tap, as the note's date).
 */
@Composable
fun DateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    supportingText: String? = null,
    today: LocalDate? = null,
    asTag: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val text = when (value) {
        null -> ""
        today -> stringResource(R.string.ui_today)
        else -> Formats.date(value)
    }
    if (asTag) {
        Tag(
            text,
            modifier.semantics { contentDescription = "$label: $text" },
            icon = PrumoIcons.Event,
            onClick = { open = true },
        )
    } else {
        ClickableField(
            text = text,
            label = label,
            icon = PrumoIcons.CalendarMonth,
            onClick = { open = true },
            modifier = modifier,
            supportingText = supportingText,
            trailing = if (value != null && onClear != null) {
                {
                    IconButton(onClick = onClear) {
                        Icon(painterResource(PrumoIcons.Close), contentDescription = stringResource(R.string.ui_clear))
                    }
                }
            } else {
                null
            },
        )
    }
    if (open) {
        DateDialog(
            initial = value ?: LocalDate.now(),
            onPick = {
                onValueChange(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** An optional time of day ("Hora"): empty until picked; [onClear] empties it. */
@Composable
fun TimeField(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    onClear: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ClickableField(
        text = value?.let(Formats::time).orEmpty(),
        label = label,
        icon = PrumoIcons.Schedule,
        onClick = { open = true },
        modifier = modifier,
        trailing = if (value != null) {
            {
                IconButton(onClick = onClear) {
                    Icon(painterResource(PrumoIcons.Close), contentDescription = stringResource(R.string.ui_clear))
                }
            }
        } else {
            null
        },
    )
    if (open) {
        TimeDialog(
            initial = value ?: LocalTime.NOON,
            onPick = {
                onValueChange(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

private enum class PickerStep { Closed, Date, Time }

/** A read-only field that does something when tapped (opens a picker). */
@Composable
private fun ClickableField(
    text: String,
    label: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
            trailingIcon = trailing,
            supportingText = supportingText?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Over the text only, so the trailing button stays tappable.
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (trailing != null) Spacing.xxl else Spacing.xxs)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        )
    }
}

@Composable
private fun DateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDate = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.getSelectedDate()?.let(onPick) }, enabled = state.getSelectedDate() != null) {
                Text(stringResource(R.string.ui_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    ) {
        DatePicker(state)
    }
}

@Composable
private fun TimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.ui_ok))
            }
        },
        title = { Text(stringResource(R.string.ui_pick_time)) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    ) {
        TimePicker(state)
    }
}

@PhonePreviews
@Composable
private fun FieldsPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ValueField("4636", {}, "Valor total", MoneyTransformation("BRL"), large = true)
                ValueField("18512", {}, "Km no painel", IntegerTransformation)
                DateTimeField(
                    LocalDateTime.of(2026, 11, 17, 14, 30),
                    {},
                    "Data e hora",
                    today = LocalDate.of(2026, 11, 17),
                )
                DateField(LocalDate.of(2027, 2, 17), {}, "Próxima troca", onClear = {})
            }
        }
    }
}
