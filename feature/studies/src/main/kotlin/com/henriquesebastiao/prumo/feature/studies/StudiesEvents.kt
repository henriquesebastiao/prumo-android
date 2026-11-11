package com.henriquesebastiao.prumo.feature.studies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.long
import com.henriquesebastiao.prumo.core.model.text
import com.henriquesebastiao.prumo.core.ui.relativeDateTime
import com.henriquesebastiao.prumo.feature.studies.ui.SubjectDetailKey
import com.henriquesebastiao.prumo.feature.studies.ui.minutesText

/**
 * "Atividade recente" of Estudos (docs/api.md of the server, "Itens do Início"), as the web's
 * studies/events.ts writes it: a session ("Estudo · FastAPI", its topic and minutes), opening the
 * subject.
 */
@Composable
@ReadOnlyComposable
internal fun studiesActivity(item: ActivityItem, context: HomeContext): HomeEvent? {
    if (item.kind != "studies.session") return null
    val data = item.data
    val whenText = relativeDateTime(item.occurredAt, context.zone, context.today)
    val minutes = data.long("duration_minutes")?.toInt()
    return HomeEvent(
        title = stringResource(R.string.studies_event_session, item.subject),
        subtitle = data.text("topic")?.let { stringResource(R.string.studies_join, whenText, it) } ?: whenText,
        value = minutes?.let { minutesText(it) },
        icon = PrumoIcons.School,
        target = item.entityId?.let(::SubjectDetailKey),
    )
}
