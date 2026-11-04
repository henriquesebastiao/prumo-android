package com.henriquesebastiao.prumo.feature.finance.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class FinanceModule {
    @Binds
    @IntoSet
    abstract fun feature(feature: FinanceFeature): PrumoFeature
}
