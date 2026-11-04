package com.henriquesebastiao.prumo.core.feature

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import java.time.LocalDate
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
}

/** What a Home card needs from the account: the currency and the user's today (S3, F9). */
data class HomeContext(val currency: String, val today: LocalDate)

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
