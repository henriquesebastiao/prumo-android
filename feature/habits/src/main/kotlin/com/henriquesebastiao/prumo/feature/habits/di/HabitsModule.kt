package com.henriquesebastiao.prumo.feature.habits.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.data.DefaultHabitsRepository
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class HabitsModule {
    @Binds
    @IntoSet
    abstract fun feature(feature: HabitsFeature): PrumoFeature

    @Binds
    abstract fun repository(repository: DefaultHabitsRepository): HabitsRepository
}
