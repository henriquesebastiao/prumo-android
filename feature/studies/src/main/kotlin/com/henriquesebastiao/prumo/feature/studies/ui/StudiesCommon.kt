package com.henriquesebastiao.prumo.feature.studies.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SeededAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.ui.errorText
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.model.DayStatus
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import java.io.Serializable
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** The user's zone and "today" in it (S3 of the server): the dates of the forms and of the screens. */
data class StudiesContext(
    val zone: ZoneId = Preferences.Default.zone,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
)

internal fun Preferences?.studiesContext(clock: Clock): StudiesContext {
    val prefs = this ?: Preferences.Default
    return StudiesContext(prefs.zone, prefs.today(clock.instant()))
}

/** "45 min", "1h", "1h 30": how the module writes minutes (as the web's). */
@Composable
@ReadOnlyComposable
internal fun minutesText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.studies_minutes, rest)
        rest == 0 -> stringResource(R.string.studies_hours, hours)
        else -> stringResource(R.string.studies_hours_minutes, hours, rest)
    }
}

private const val MINUTES_PER_HOUR = 60

/** "Cumprido", "Parcial"… (E4). */
@Composable
@ReadOnlyComposable
internal fun DayStatus.label(): String = stringResource(
    when (this) {
        DayStatus.Met -> R.string.studies_status_met
        DayStatus.Partial -> R.string.studies_status_partial
        DayStatus.Missed -> R.string.studies_status_missed
        DayStatus.InProgress -> R.string.studies_status_in_progress
        DayStatus.Extra -> R.string.studies_status_extra
    },
)

/** The chip's tone of a status: extra study is good news, as a goal met. */
internal val DayStatus.tone: StatusTone
    get() = when (this) {
        DayStatus.Met, DayStatus.Extra -> StatusTone.Success
        DayStatus.Partial -> StatusTone.Warning
        DayStatus.Missed -> StatusTone.Error
        DayStatus.InProgress -> StatusTone.Neutral
    }

@Composable
@ReadOnlyComposable
internal fun StudyColor.label(): String = stringResource(
    when (this) {
        StudyColor.Green -> R.string.studies_color_green
        StudyColor.Teal -> R.string.studies_color_teal
        StudyColor.Cyan -> R.string.studies_color_cyan
        StudyColor.Blue -> R.string.studies_color_blue
        StudyColor.Indigo -> R.string.studies_color_indigo
        StudyColor.Violet -> R.string.studies_color_violet
        StudyColor.Pink -> R.string.studies_color_pink
        StudyColor.Red -> R.string.studies_color_red
        StudyColor.Orange -> R.string.studies_color_orange
        StudyColor.Amber -> R.string.studies_color_amber
        StudyColor.Gray -> R.string.studies_color_gray
    },
)

/** A subject or an area: the cap in the area's color. */
@Composable
internal fun StudyAvatar(color: StudyColor, modifier: Modifier = Modifier, size: Int = 40) {
    SeededAvatar(color.seed, PrumoIcons.School, modifier = modifier, size = size.dp, iconSize = (size * 0.6f).dp)
}

/**
 * Opens a subject's note in the app that handles `obsidian://` (E8, D24). False when no app on the
 * device opens it: the screen says so.
 */
internal fun openObsidian(context: Context, uri: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** Which sheet of Estudos is open over a screen, and for what. Kept across a rotation. */
data class SheetRequest(
    val kind: SheetKind,
    val id: String? = null,
    /** The subject a new session or the area a new subject starts with. */
    val parentId: String? = null,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

enum class SheetKind { Session, Area, Subject }

/** The sheet of [request], in the module's colors (also over the Home). */
@Composable
internal fun StudiesSheet(request: SheetRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    ModuleTheme(StudiesFeature.SEED) {
        when (request.kind) {
            SheetKind.Session -> SessionSheet(request.id, request.parentId, onDismiss)
            SheetKind.Area -> AreaSheet(request.id, onDismiss)
            SheetKind.Subject -> SubjectSheet(request.id, request.parentId, onDismiss)
        }
    }
}

/** What the toolbar of Estudos records. [onNewSubject] null leaves it out. */
internal class StudiesActions(val onLog: () -> Unit, val onNewSubject: (() -> Unit)? = null)

/**
 * The floating toolbar of Estudos (D24): the FAB registers a study, a button adds a subject. Without
 * that button (a subject's screen) only the FAB shows.
 */
@Composable
internal fun StudiesToolbar(actions: StudiesActions, modifier: Modifier = Modifier) {
    val icon: @Composable () -> Unit = {
        Icon(painterResource(PrumoIcons.Timer), contentDescription = stringResource(R.string.studies_action_log))
    }
    val onNew = actions.onNewSubject
    if (onNew == null) {
        FloatingActionButton(
            onClick = actions.onLog,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            content = icon,
        )
        return
    }
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = actions.onLog,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                content = icon,
            )
        },
        // A module's toolbar is the standard one (the Home's is vibrant), its FAB in primary container.
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = onNew) {
            Icon(
                painterResource(PrumoIcons.LibraryAdd),
                contentDescription = stringResource(R.string.studies_new_subject),
            )
        }
    }
}

/**
 * The subjects as filter chips in a row that scrolls, opened at the [selected] one (it may be far
 * down the list: the last one used, a subject's own sheet).
 */
@Composable
internal fun SubjectChips(
    subjects: List<SubjectRef>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(subjects.indexOfFirst { it.id == selected }.coerceAtLeast(0))
    LazyRow(modifier, state = state, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(subjects, key = { it.id }) { option ->
            FilterChip(
                selected = option.id == selected,
                onClick = { onSelect(option.id) },
                label = { Text(option.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { StudyAvatar(option.color, size = 18) },
            )
        }
    }
}

/** Room under a list for the floating toolbar. */
internal val TOOLBAR_SPACE = 112.dp

/** The text of an error of Estudos: the module's codes (`docs/api.md` of the server), then the app's. */
@Composable
@ReadOnlyComposable
internal fun studiesErrorText(error: AppError): String = when ((error as? AppError.Api)?.code) {
    INVALID_OBSIDIAN_URI -> stringResource(R.string.studies_error_invalid_obsidian_uri)
    PLAN_LOCKED -> stringResource(R.string.studies_error_plan_locked)
    PLAN_DATE_TAKEN -> stringResource(R.string.studies_error_plan_date_taken)
    SUBJECT_ARCHIVED -> stringResource(R.string.studies_error_subject_archived)
    AREA_IN_USE -> stringResource(R.string.studies_error_area_in_use)
    TOPIC_IN_USE -> stringResource(R.string.studies_error_topic_in_use)
    else -> errorText(error)
}

/** The message under the field at [path] when the server refused it, or null. */
@Composable
@ReadOnlyComposable
internal fun fieldError(error: AppError?, path: String): String? {
    val api = error as? AppError.Api ?: return null
    if (api.code == INVALID_OBSIDIAN_URI && path == "obsidian_uri") {
        return stringResource(R.string.studies_error_invalid_obsidian_uri)
    }
    if (api.code == PLAN_DATE_TAKEN &&
        path == "valid_from"
    ) {
        return stringResource(R.string.studies_error_plan_date_taken)
    }
    val refused = api.fields.any { it.path == path || it.path.startsWith("$path.") }
    return if (refused) stringResource(R.string.studies_field_invalid) else null
}

/** Whether [error] belongs to a field (shown there) rather than to the whole form. */
internal fun AppError.isFieldError(): Boolean =
    this is AppError.Api && (code == INVALID_OBSIDIAN_URI || code == PLAN_DATE_TAKEN || fields.isNotEmpty())

private const val INVALID_OBSIDIAN_URI = "studies.invalid_obsidian_uri"
private const val PLAN_LOCKED = "studies.plan_locked"
private const val PLAN_DATE_TAKEN = "studies.plan_date_taken"
private const val SUBJECT_ARCHIVED = "studies.subject_archived"
private const val AREA_IN_USE = "studies.area_in_use"
private const val TOPIC_IN_USE = "studies.topic_in_use"
