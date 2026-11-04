package com.henriquesebastiao.prumo.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * "A module's data changed on the server": each module's repository calls [changed] after a write
 * (a fueling saved from the Home's sheet, a vehicle archived), and whoever shows that data (the
 * Home's summaries, the module's open screens) loads it again. Values are module ids.
 */
@Singleton
class DataChanges @Inject constructor() {
    private val flow = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    val changes: SharedFlow<String> = flow.asSharedFlow()

    fun changed(module: String) {
        flow.tryEmit(module)
    }

    private companion object {
        const val BUFFER = 16
    }
}
