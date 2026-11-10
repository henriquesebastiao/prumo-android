package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.core.designsystem.component.AutocompleteField
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.Suggestion
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.DateTimeField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.model.OilChangeSuggestion
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import java.time.LocalDate
import java.time.LocalDateTime

/** What the maintenance form does when something changes. */
internal class MaintenanceEditorEvents(
    val onSelect: (String) -> Unit,
    val onFieldsChange: (MaintenanceFields) -> Unit,
    val onDateTimeChange: (LocalDateTime) -> Unit,
    val onPickWorkshop: (Workshop) -> Unit,
    val onNextOnChange: (LocalDate?) -> Unit,
    val onPartChange: (PartText) -> Unit,
    val onRemovePart: (Long) -> Unit,
    val onAddPart: () -> Unit,
    val onSave: () -> Unit,
)

@Composable
internal fun MaintenanceEditorRoute(
    key: MaintenanceEditor,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: MaintenanceEditorViewModel =
        hiltViewModel<MaintenanceEditorViewModel, MaintenanceEditorViewModel.Factory>(
            creationCallback = { it.create(key) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    MaintenanceEditorScreen(
        state = state,
        fields = viewModel.fields,
        canSave = viewModel.canSave,
        events = MaintenanceEditorEvents(
            onSelect = viewModel::select,
            onFieldsChange = viewModel::onFieldsChange,
            onDateTimeChange = viewModel::onDateTimeChange,
            onPickWorkshop = viewModel::pickWorkshop,
            onNextOnChange = viewModel::onNextOnChange,
            onPartChange = viewModel::onPartChange,
            onRemovePart = viewModel::removePart,
            onAddPart = viewModel::addPart,
            onSave = viewModel::save,
        ),
        onRetry = viewModel::load,
        onClose = navigator::back,
        modifier = modifier,
    )
}

/** A maintenance (boards 10 and 11) or an oil change (board 12), new or edited. */
@Composable
internal fun MaintenanceEditorScreen(
    state: MaintenanceEditorUiState,
    fields: MaintenanceFields,
    canSave: Boolean,
    events: MaintenanceEditorEvents,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        state.editing == null && state.oilChange -> R.string.garage_action_oil_change
        state.editing == null -> R.string.garage_action_maintenance
        state.oilChange -> R.string.garage_oil_change_edit
        else -> R.string.garage_maintenance_edit
    }
    Scaffold(
        modifier,
        topBar = { PrumoTopBar(stringResource(title), onBack = onClose, close = true) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.garage_loading))

                state.error != null -> ErrorState(state.error, onRetry = onRetry)

                state.vehicleId == null -> GroupNote(
                    stringResource(R.string.garage_no_vehicles_for_record),
                    Modifier.padding(Spacing.screen),
                )

                else -> MaintenanceForm(state, fields, canSave, events)
            }
        }
    }
}

@Composable
private fun MaintenanceForm(
    state: MaintenanceEditorUiState,
    fields: MaintenanceFields,
    canSave: Boolean,
    events: MaintenanceEditorEvents,
) {
    val error = state.saveError
    val currency = state.context.currency
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.editing == null) VehicleChoice(state.vehicles, state.vehicleId, events.onSelect)
        // Boards 10 and 12: date and km side by side.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DateTimeField(
                value = state.performedAt,
                onValueChange = events.onDateTimeChange,
                label = stringResource(R.string.garage_date_time),
                today = state.context.today,
                modifier = Modifier.weight(1.3f),
            )
            ValueField(
                digits = fields.km,
                onDigitsChange = { events.onFieldsChange(fields.copy(km = it)) },
                label = stringResource(R.string.garage_km_at),
                transformation = IntegerTransformation,
                suffix = stringResource(R.string.garage_km_unit),
                error = kmError(error, state.context),
                modifier = Modifier.weight(1f),
            )
        }
        WorkshopField(state, fields, events)
        if (state.oilChange) OilFields(state, fields, events)
        SectionHeader(
            stringResource(R.string.garage_parts),
            trailing = fields.parts.count { !it.isEmpty }.takeIf { it > 0 }?.let {
                pluralStringResource(R.plurals.garage_parts_count, it, it)
            },
        )
        PartsEditor(fields.parts, currency, error, events.onPartChange, events.onRemovePart, events.onAddPart)
        ValueField(
            digits = fields.labor,
            onDigitsChange = { events.onFieldsChange(fields.copy(labor = it)) },
            label = stringResource(R.string.garage_labor),
            transformation = MoneyTransformation(currency),
            error = fieldError(error, "labor_cost_cents"),
        )
        // C3 (D15): a preview of the total; the total saved is the server's (G4).
        val (parts, labor) = fields.previewTotals()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(
                stringResource(R.string.garage_total_preview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FitText(Formats.money(parts + labor, currency), style = PrumoType.dataHeadline)
        }
        GroupNote(
            stringResource(R.string.garage_total_hint, Formats.money(parts, currency), Formats.money(labor, currency)),
        )
        OutlinedTextField(
            value = fields.notes,
            onValueChange = { events.onFieldsChange(fields.copy(notes = it)) },
            label = { Text(stringResource(R.string.garage_notes)) },
            placeholder = { Text(stringResource(R.string.garage_notes_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null && !error.isFieldError()) {
            Text(garageErrorText(error), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = events.onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (state.oilChange) R.string.garage_oil_change_save else R.string.garage_maintenance_save,
                ),
            )
        }
    }
}

/** The workshop: one of the user's, or a new name the server creates on save (G10). */
@Composable
private fun WorkshopField(state: MaintenanceEditorUiState, fields: MaintenanceFields, events: MaintenanceEditorEvents) {
    val typed = fields.workshop.trim()
    val matches = state.workshops
        .filter { typed.isEmpty() || it.name.contains(typed, ignoreCase = true) }
        .take(MAX_SUGGESTIONS)
    val exact = state.workshops.any { it.name.equals(typed, ignoreCase = true) }
    val isNew = typed.isNotEmpty() && !exact
    AutocompleteField(
        value = fields.workshop,
        onValueChange = { events.onFieldsChange(fields.copy(workshop = it)) },
        label = stringResource(R.string.garage_workshop),
        suggestions = matches.map {
            Suggestion(
                it.name,
                pluralStringResource(R.plurals.garage_maintenances_count, it.maintenancesCount, it.maintenancesCount),
            )
        },
        onPick = { index -> events.onPickWorkshop(matches[index]) },
        leadingIcon = PrumoIcons.Storefront,
        createLabel = if (isNew) stringResource(R.string.garage_workshop_create, typed) else null,
        supportingText = if (isNew) stringResource(R.string.garage_workshop_new_hint) else null,
        error = fieldError(state.saveError, "workshop_name"),
        capitalization = KeyboardCapitalization.Words,
    )
}

/** The oil used (G14) and the next change, suggested by the interval and editable for this change (G5). */
@Composable
private fun OilFields(state: MaintenanceEditorUiState, fields: MaintenanceFields, events: MaintenanceEditorEvents) {
    val typed = fields.oilType.trim()
    val oils = state.suggestion?.previousOilTypes.orEmpty()
        .filter { (typed.isEmpty() || it.contains(typed, ignoreCase = true)) && !it.equals(typed, ignoreCase = true) }
    AutocompleteField(
        value = fields.oilType,
        onValueChange = { events.onFieldsChange(fields.copy(oilType = it)) },
        label = stringResource(R.string.garage_oil_type),
        suggestions = oils.map { Suggestion(it, icon = PrumoIcons.History) },
        onPick = { index -> events.onFieldsChange(fields.copy(oilType = oils[index])) },
        leadingIcon = PrumoIcons.OilBarrel,
        error = fieldError(state.saveError, "oil_type"),
    )
    // Board 12: the next change in the hero card, km and date side by side.
    SectionCard(
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.OilBarrel,
        overline = stringResource(R.string.garage_next_change_suggested),
        title = stringResource(R.string.garage_next_change),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ValueField(
                    digits = fields.nextKm,
                    onDigitsChange = { events.onFieldsChange(fields.copy(nextKm = it)) },
                    label = stringResource(R.string.garage_next_change_km),
                    transformation = IntegerTransformation,
                    suffix = stringResource(R.string.garage_km_unit),
                    error = fieldError(state.saveError, "next_oil_change_km"),
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    value = state.nextOn,
                    onValueChange = events.onNextOnChange,
                    label = stringResource(R.string.garage_next_change_date),
                    onClear = { events.onNextOnChange(null) },
                    modifier = Modifier.weight(1f),
                )
            }
            intervalNote(state.suggestion)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "A cada 1.000 km ou 3 meses, como no cadastro do veículo. Pode editar só para esta troca." */
@Composable
private fun intervalNote(suggestion: OilChangeSuggestion?): String? {
    if (suggestion == null) return null
    val km = suggestion.intervalKm?.let(Formats::km)
    val months = suggestion.intervalMonths?.let { pluralStringResource(R.plurals.garage_months, it, it) }
    return when {
        km != null && months != null -> stringResource(R.string.garage_interval_note_both, km, months)
        km != null -> stringResource(R.string.garage_interval_note, km)
        months != null -> stringResource(R.string.garage_interval_note, months)
        else -> stringResource(R.string.garage_interval_none)
    }
}

/** The parts of a maintenance (C2: with quantity), added and removed in place. */
@Composable
private fun PartsEditor(
    parts: List<PartText>,
    currency: String,
    error: AppError?,
    onChange: (PartText) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        parts.forEachIndexed { index, part ->
            key(part.key) {
                PartRow(part, currency, fieldError(error, "items.$index"), onChange, onRemove)
            }
        }
        TextButton(onClick = onAdd) {
            Icon(painterResource(PrumoIcons.Add), contentDescription = null)
            Text(stringResource(R.string.garage_part_add), Modifier.padding(start = Spacing.sm))
        }
    }
}

@Composable
private fun PartRow(
    part: PartText,
    currency: String,
    error: String?,
    onChange: (PartText) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val missingName = part.description.isBlank() && part.price.isNotEmpty()
    val supporting = error ?: if (missingName) stringResource(R.string.garage_part_name_missing) else null
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = part.description,
                onValueChange = { onChange(part.copy(description = it)) },
                label = { Text(stringResource(R.string.garage_part)) },
                isError = supporting != null,
                supportingText = supporting?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onRemove(part.key) }) {
                Icon(
                    painterResource(PrumoIcons.DoNotDisturbOn),
                    contentDescription = stringResource(R.string.garage_part_remove),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ValueField(
                digits = part.quantity,
                onDigitsChange = { onChange(part.copy(quantity = it)) },
                label = stringResource(R.string.garage_part_quantity),
                transformation = VisualTransformation.None,
                maxDigits = QUANTITY_DIGITS,
                modifier = Modifier.weight(QUANTITY_WEIGHT),
            )
            ValueField(
                digits = part.price,
                onDigitsChange = { onChange(part.copy(price = it)) },
                label = stringResource(R.string.garage_part_price),
                transformation = MoneyTransformation(currency),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val MAX_SUGGESTIONS = 6
private const val QUANTITY_DIGITS = 5
private const val QUANTITY_WEIGHT = 0.4f

@PhonePreviews
@Composable
private fun MaintenanceEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            MaintenanceEditorScreen(
                state = MaintenanceEditorUiState(
                    loading = false,
                    oilChange = true,
                    vehicles = GaragePreview.vehicles,
                    vehicleId = GaragePreview.cg.id,
                    workshops = GaragePreview.workshops,
                    suggestion = OilChangeSuggestion(
                        nextKm = 19_420,
                        nextOn = LocalDate.of(2027, 2, 17),
                        intervalKm = 1_000,
                        intervalMonths = 3,
                        paceKmPerDay = 29.7,
                        estimatedKmOn = null,
                        previousOilTypes = listOf("4T 20W-50 semissintético", "4T 10W-30 sintético"),
                    ),
                    nextOn = LocalDate.of(2027, 2, 17),
                ),
                fields = MaintenanceFields(
                    km = "18420",
                    workshop = "Moto Peças Rocha",
                    labor = "1500",
                    oilType = "4T 20W-50 semissintético",
                    nextKm = "19420",
                    parts = listOf(PartText(0, "Óleo 4T 20W-50", "1", "3890")),
                ),
                canSave = true,
                events = MaintenanceEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onRetry = {},
                onClose = {},
            )
        }
    }
}
