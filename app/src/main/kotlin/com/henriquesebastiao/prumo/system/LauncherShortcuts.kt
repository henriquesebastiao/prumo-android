package com.henriquesebastiao.prumo.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.henriquesebastiao.prumo.MainActivity
import com.henriquesebastiao.prumo.core.feature.QuickAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shortcuts of the app's icon (a long press on the launcher): the quick actions of the enabled
 * modules that ask for one ([QuickAction.launcherShortcut], "Marcar hábitos"). Dynamic, so they
 * follow the enabled modules and leave with the session. A tap opens [MainActivity] with
 * [ACTION_QUICK_ACTION] and the action's id, and the Home opens its sheet.
 */
interface LauncherShortcuts {
    /** The shortcuts of [actions] that ask for one, in place of the previous ones. */
    fun publish(actions: List<QuickAction>)

    fun clear()

    companion object {
        const val ACTION_QUICK_ACTION = QuickAction.LAUNCH_ACTION
        const val EXTRA_ACTION_ID = QuickAction.EXTRA_ID

        fun intent(context: Context, actionId: String): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_QUICK_ACTION)
            .putExtra(EXTRA_ACTION_ID, actionId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** The quick action an intent of a shortcut asks for, or null. */
        fun actionOf(intent: Intent?): String? =
            intent?.takeIf { it.action == ACTION_QUICK_ACTION }?.getStringExtra(EXTRA_ACTION_ID)
    }
}

/** [LauncherShortcuts] through the system's ShortcutManager. */
@Singleton
class SystemLauncherShortcuts @Inject constructor(@ApplicationContext private val context: Context) :
    LauncherShortcuts {
    override fun publish(actions: List<QuickAction>) {
        val shortcuts = actions.filter { it.launcherShortcut }.map { action ->
            ShortcutInfoCompat.Builder(context, action.id)
                .setShortLabel(context.getString(action.labelRes))
                .setLongLabel(context.getString(action.labelRes))
                .setIcon(IconCompat.createWithResource(context, action.icon))
                .setIntent(LauncherShortcuts.intent(context, action.id))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    override fun clear() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }
}
