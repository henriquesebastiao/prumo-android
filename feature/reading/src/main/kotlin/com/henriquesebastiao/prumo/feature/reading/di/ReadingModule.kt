package com.henriquesebastiao.prumo.feature.reading.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.DefaultReadingRepository
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ReadingModule {
    @Binds
    @IntoSet
    abstract fun feature(feature: ReadingFeature): PrumoFeature

    @Binds
    abstract fun repository(repository: DefaultReadingRepository): ReadingRepository
}
