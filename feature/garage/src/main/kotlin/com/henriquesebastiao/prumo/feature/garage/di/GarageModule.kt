package com.henriquesebastiao.prumo.feature.garage.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.data.DefaultGarageRepository
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class GarageModule {
    @Binds
    @IntoSet
    abstract fun feature(feature: GarageFeature): PrumoFeature

    @Binds
    abstract fun repository(repository: DefaultGarageRepository): GarageRepository
}
