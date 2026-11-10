package com.henriquesebastiao.prumo.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PrumoDatabase =
        Room.databaseBuilder(context, PrumoDatabase::class.java, PrumoDatabase.NAME).build()

    @Provides
    fun responses(database: PrumoDatabase): ResponseDao = database.responses()
}
