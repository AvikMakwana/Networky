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


/**
 * Module responsible for providing Coroutine-related dependencies.
 * Installed in SingletonComponent to create application-scoped CoroutineScope.
 */
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
        // Use SupervisorJob for the application scope to prevent children failures from cancelling the entire scope.
        return CoroutineScope(SupervisorJob() + ioDispatcher)
    }
}

/**
 * Module responsible for binding interfaces to their concrete implementations.
 * Installed in SingletonComponent.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorBindsModule {
    // Binds the internal repository contract to its implementation.
    @Binds
    @Singleton
    internal abstract fun bindOSNetworkMonitorRepository(
        impl: DefaultNetworkMonitorRepository
    ): OSNetworkMonitorRepository

    // Binds the Use Case contract to its implementation.
    @Binds
    @Singleton
    internal abstract fun bindCheckServerHealthUseCase(
        impl: CheckServerHealthUseCaseImpl
    ): CheckServerHealthUseCase

    // Binds the public controller interface to the internal monitor implementation.
    @Binds
    @Singleton
    internal abstract fun bindNetworkMonitorController(
        impl: GlobalStatusMonitor
    ): NetworkMonitorController
}

/**
 * Module responsible for providing the NetworkMonitorConfig.
 * This module is crucial as the host app must override this function to provide its custom config.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkMonitorProvidesModule {
    @Provides
    @Singleton
    fun provideNetworkMonitorConfig(): NetworkMonitorConfig {
        // Default configuration provided by the library.
        return NetworkMonitorConfig()
    }
}
