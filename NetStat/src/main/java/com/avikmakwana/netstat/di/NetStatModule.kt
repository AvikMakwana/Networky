package com.avikmakwana.netstat.di

import com.avikmakwana.netstat.data.monitor.DefaultNetworkMonitorRepository
import com.avikmakwana.netstat.data.monitor.OSNetworkMonitorRepository
import com.avikmakwana.netstat.domain.monitor.GlobalStatusMonitor
import com.avikmakwana.netstat.domain.monitor.NetworkMonitorConfig
import com.avikmakwana.netstat.domain.monitor.NetworkMonitorController
import com.avikmakwana.netstat.domain.usecases.CheckServerHealthUseCase
import com.avikmakwana.netstat.domain.usecases.CheckServerHealthUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IODispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope


@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    @IODispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IODispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope {
        return CoroutineScope(SupervisorJob() + ioDispatcher)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorBindsModule {
    @Binds
    @Singleton
    internal abstract fun bindOSNetworkMonitorRepository(
        impl: DefaultNetworkMonitorRepository
    ): OSNetworkMonitorRepository

    @Binds
    @Singleton
    internal abstract fun bindCheckServerHealthUseCase(
        impl: CheckServerHealthUseCaseImpl
    ): CheckServerHealthUseCase

    @Binds
    @Singleton
    internal abstract fun bindNetworkMonitorController(
        impl: GlobalStatusMonitor
    ): NetworkMonitorController
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkMonitorProvidesModule {
    @Provides
    @Singleton
    fun provideNetworkMonitorConfig(): NetworkMonitorConfig {
        return NetworkMonitorConfig()
    }
}
