package com.henriquesebastiao.prumo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** "Now", replaceable in tests: the greeting and "today" come from it, in the user's zone. */
    @Provides
    fun clock(): Clock = Clock.systemUTC()
}
