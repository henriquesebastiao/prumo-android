package com.henriquesebastiao.prumo.feature.studies.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.data.DefaultStudiesRepository
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class StudiesModule {
    @Binds
    @IntoSet
    abstract fun feature(feature: StudiesFeature): PrumoFeature

    @Binds
    abstract fun repository(repository: DefaultStudiesRepository): StudiesRepository
}
