package com.avikmakwana.netstat.domain.monitor

import com.avikmakwana.netstat.data.monitor.NetworkReachability
import com.avikmakwana.netstat.data.monitor.OSNetworkMonitorRepository
import com.avikmakwana.netstat.di.ApplicationScope
import com.avikmakwana.netstat.domain.usecases.CheckServerHealthUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * MANDATE: The central logic class. It fuses the OS state, the server check, and manages
 * the polling lifecycle with exponential backoff and contextual pausing.
 *
 * CRITICAL FIX: Marked as 'internal' because its constructor exposes the 'internal'
 * OSNetworkMonitorRepository type, satisfying Kotlin visibility rules.
 * The host app only interacts with the public NetworkMonitorController interface.
 */
@Singleton
internal class GlobalStatusMonitor @Inject constructor(
    private val config: NetworkMonitorConfig,
    private val osMonitor: OSNetworkMonitorRepository,
    private val healthCheckUseCase: CheckServerHealthUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope
) : NetworkMonitorController {
    private val _isHighPriorityActive = MutableStateFlow(false)
    private val _serverRtt = MutableStateFlow(0L)
    private var currentPollingDelay = config.pollingIntervalMinimum
    private var pollingJob: Job? = null

    /**
     * MANDATE: Implements the single source of truth by fusing OS state and server state.
     */
    override val status: StateFlow<GlobalReachabilityState> = osMonitor.osNetworkStatus
        .combine(_serverRtt) { osStatus, rtt ->
            when (osStatus) {
                NetworkReachability.Disconnected -> GlobalReachabilityState.NoNetwork
                NetworkReachability.ConnectedButNoInternet -> GlobalReachabilityState.InternetBlocked

                NetworkReachability.Available -> {
                    when {
                        rtt > 0L -> {
                            if (rtt.milliseconds > config.maxLatencyThreshold) {
                                GlobalReachabilityState.DegradedLatency(rtt)
                            } else {
                                GlobalReachabilityState.FullyAvailable
                            }
                        }

                        else -> GlobalReachabilityState.ServerUnreachable
                    }
                }
            }
        }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = GlobalReachabilityState.Checking
        )

    /**
     * Implements the control API. Idempotent function to pause/resume polling.
     */
    override fun setHighPriorityActive(isActive: Boolean) {
        _isHighPriorityActive.value = isActive

        if (isActive) {
            pollingJob?.cancel()
            pollingJob = null
            _serverRtt.value = 1L
        } else {
            startPollingLoop()
        }
    }

    init {
        applicationScope.launch {
            osMonitor.osNetworkStatus.collect { osStatus ->
                when (osStatus) {
                    NetworkReachability.Available -> {
                        if (!_isHighPriorityActive.value) {
                            startPollingLoop()
                        }
                    }

                    else -> {
                        pollingJob?.cancel()
                        pollingJob = null
                        _serverRtt.value = 0L
                    }
                }
            }
        }
    }

    /**
     * Launches the continuous polling job, managing delays and backoff.
     */
    private fun startPollingLoop() {
        if (pollingJob?.isActive == true) return

        pollingJob = applicationScope.launch {
            while (true) {
                val rtt = healthCheckUseCase()

                _serverRtt.value = rtt

                if (rtt == 0L) {
                    currentPollingDelay =
                        (currentPollingDelay * 2).coerceAtMost(config.pollingIntervalMaximum)
                } else {
                    currentPollingDelay = config.pollingIntervalMinimum
                }
                delay(currentPollingDelay)
            }
        }
    }
}
