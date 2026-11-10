package com.henriquesebastiao.prumo.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DashboardRepository
import com.henriquesebastiao.prumo.core.data.repository.DataExportRepository
import com.henriquesebastiao.prumo.core.data.repository.DataStoreDevicePreferences
import com.henriquesebastiao.prumo.core.data.repository.DefaultAccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DefaultDashboardRepository
import com.henriquesebastiao.prumo.core.data.repository.DefaultDataExportRepository
import com.henriquesebastiao.prumo.core.data.repository.DefaultServerRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.repository.ServerRepository
import com.henriquesebastiao.prumo.core.data.session.AndroidDeviceInfo
import com.henriquesebastiao.prumo.core.data.session.DefaultSessionRepository
import com.henriquesebastiao.prumo.core.data.session.DeviceInfo
import com.henriquesebastiao.prumo.core.data.session.KeystoreTokenCipher
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.TokenCipher
import com.henriquesebastiao.prumo.core.network.SessionGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The scope of work that lives as long as the app (not a screen's). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The DataStore of the session (`session.preferences_pb`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionPreferences

/** The DataStore of the device's preferences (`device.preferences_pb`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevicePreferencesStore

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @SessionPreferences
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }

    @Provides
    @Singleton
    @DevicePreferencesStore
    fun deviceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("device") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    abstract fun sessionRepository(impl: DefaultSessionRepository): SessionRepository

    @Binds
    abstract fun sessionGateway(impl: DefaultSessionRepository): SessionGateway

    @Binds
    abstract fun tokenCipher(impl: KeystoreTokenCipher): TokenCipher

    @Binds
    abstract fun deviceInfo(impl: AndroidDeviceInfo): DeviceInfo

    @Binds
    abstract fun serverRepository(impl: DefaultServerRepository): ServerRepository

    @Binds
    abstract fun accountRepository(impl: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun dashboardRepository(impl: DefaultDashboardRepository): DashboardRepository

    @Binds
    abstract fun devicePreferences(impl: DataStoreDevicePreferences): DevicePreferencesRepository

    @Binds
    abstract fun dataExport(impl: DefaultDataExportRepository): DataExportRepository
}
