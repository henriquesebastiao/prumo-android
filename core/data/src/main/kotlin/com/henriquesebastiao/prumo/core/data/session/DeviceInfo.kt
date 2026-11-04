package com.henriquesebastiao.prumo.core.data.session

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** What the server shows in the session list for this device. */
fun interface DeviceInfo {
    /** The device's name: the one set in the system's settings ("Pixel 9 da Marina"), or the model. */
    fun name(): String
}

class AndroidDeviceInfo @Inject constructor(@ApplicationContext private val context: Context) : DeviceInfo {
    override fun name(): String {
        val named = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        return named?.takeIf { it.isNotBlank() }?.take(MAX_LENGTH) ?: Build.MODEL.take(MAX_LENGTH)
    }

    private companion object {
        // LoginRequest.device_name accepts up to 120 characters.
        const val MAX_LENGTH = 120
    }
}
