package com.henriquesebastiao.prumo.di

import com.henriquesebastiao.prumo.system.LauncherShortcuts
import com.henriquesebastiao.prumo.system.SystemLauncherShortcuts
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShortcutsModule {
    /** The shortcuts of the app's icon, replaceable in tests. */
    @Binds
    abstract fun launcherShortcuts(shortcuts: SystemLauncherShortcuts): LauncherShortcuts
}
