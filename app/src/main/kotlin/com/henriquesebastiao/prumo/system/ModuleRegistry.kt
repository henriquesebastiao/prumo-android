package com.henriquesebastiao.prumo.system

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.model.InstalledModule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The modules the app knows (each `:feature:*`'s PrumoFeature, from Hilt) and how they meet the
 * server's: a module the server has and the app doesn't know is ignored; one the app knows and the
 * server doesn't have, or that the user disabled, is hidden. The order is the user's.
 */
@Singleton
class ModuleRegistry @Inject constructor(features: Set<@JvmSuppressWildcards PrumoFeature>) {
    /** The known modules, by id. */
    val all: Map<String, PrumoFeature> = features.associateBy { it.id }

    /** The known modules among [enabledIds] (the preference, already in the user's order). */
    fun enabled(enabledIds: List<String>): List<PrumoFeature> = enabledIds.mapNotNull { all[it] }

    /** The installed modules the app knows, in the server's order, with whether each is on. */
    fun installed(modules: List<InstalledModule>): List<Pair<PrumoFeature, InstalledModule>> =
        modules.sortedBy { it.position }.mapNotNull { module -> all[module.id]?.let { it to module } }

    fun byId(id: String): PrumoFeature? = all[id]
}
