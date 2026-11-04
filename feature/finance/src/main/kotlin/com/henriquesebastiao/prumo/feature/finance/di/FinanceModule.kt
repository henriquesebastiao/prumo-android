package com.henriquesebastiao.prumo.feature.finance.di

import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.data.DefaultFinanceRepository
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
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

    @Binds
    abstract fun repository(repository: DefaultFinanceRepository): FinanceRepository
}
