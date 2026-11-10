package com.henriquesebastiao.prumo.core.feature

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.JsonObject

/**
 * A module of Prumo in the app (docs/arquitetura.md, "Módulos do Prumo no app"): one per
 * `:feature:<id>`, registered in Hilt with `@IntoSet`. The server says which modules are installed,
 * enabled and in which order (`GET /api/v1/modules`); the app shows the ones it also knows.
 */
interface PrumoFeature {
    /** The module's id on the server: `finance`, `garage`, `reading`. */
    val id: String

    /** "Veículos". */
    @get:StringRes
    val nameRes: Int

    /** What it holds, under its name in Settings › Módulos: "Km, abastecimentos e manutenções". */
    @get:StringRes
    val descriptionRes: Int

    @get:DrawableRes
    val icon: Int

    /** The seed of the module's colors (ARGB): its screens and cards are drawn in `ModuleTheme(seed)`. */
    val seed: Int

    /** Where the module opens (its card on the Home). */
    val entryKey: NavKey

    /** Its actions in "Todas as ações" and on the Home's toolbar, in the module's order. */
    val quickActions: List<QuickAction>

    /** The module's destinations in the app's navigation. */
    fun EntryProviderScope<NavKey>.entries(navigator: Navigator)

    /**
     * The module's card on the Home, from its summary in `GET /dashboard` ([summary] is the JSON of
     * the module's own schema). [hero] is the first module of the user's order: full width (D08).
     * Draw it in the module's colors; a summary that doesn't decode shows the card without data.
     */
    @Composable
    fun HomeCard(summary: JsonObject?, hero: Boolean, context: HomeContext, onOpen: () -> Unit, modifier: Modifier)

    /**
     * How one of the module's items of "Vem aí" reads on the Home (D20): the server sends a stable
     * [UpcomingItem.kind] with plain values, and the module writes them. Null for a kind it doesn't
     * know (a newer server), which the Home skips.
     */
    @Composable
    fun upcoming(item: UpcomingItem, context: HomeContext): HomeEvent? = null

    /** How one of the module's items of "Atividade recente" reads on the Home (D20), as [upcoming]. */
    @Composable
    fun activity(item: ActivityItem, context: HomeContext): HomeEvent? = null
}

/** What the Home needs from the account: the currency and the user's zone and today (S3, F9). */
data class HomeContext(val currency: String, val today: LocalDate, val zone: ZoneId = Preferences.Default.zone)

/**
 * A line of "Vem aí" or "Atividade recente", written by its module: "Troca de óleo · Honda CG",
 * "Estimativa pelo ritmo de 29 km/dia". [value] goes at the end ("R$ 46,36"; in the success color
 * when [positive], an income), [icon] in the module's colors (the activity's avatar), and [target]
 * is where tapping it goes.
 */
data class HomeEvent(
    val title: String,
    val subtitle: String? = null,
    val value: String? = null,
    val positive: Boolean = false,
    @param:DrawableRes val icon: Int? = null,
    val target: NavKey? = null,
)

/**
 * A quick record of a module ("Registrar abastecimento"). With a [sheet], it opens over the current
 * screen (the Home), which closes it through `onDismiss`; without one, it opens [target]. [labelRes]
 * is the long label (toolbar, accessibility), [shortLabelRes] the one under the icon in "Todas as
 * ações".
 */
data class QuickAction(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val shortLabelRes: Int,
    @param:DrawableRes val icon: Int,
    val target: NavKey,
    val sheet: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
)

/** What a module may ask of the app's navigation. */
interface Navigator {
    fun navigate(key: NavKey)

    fun back()
}
