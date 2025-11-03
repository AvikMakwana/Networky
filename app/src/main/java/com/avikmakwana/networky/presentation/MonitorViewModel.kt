package com.avikmakwana.networky.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avikmakwana.netstat.domain.monitor.GlobalReachabilityState
import com.avikmakwana.netstat.domain.monitor.NetworkMonitorController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.to

/**
 * Represents the simplified UI state derived from the complex GlobalReachabilityState.
 */
data class MonitorViewState(
    val statusText: String,
    val statusColor: Color,
    val isSocketActive: Boolean
)

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val networkMonitorController: NetworkMonitorController
) : ViewModel() {

    private var isSocketConnected = false

    private val _viewState = MutableStateFlow(
        MonitorViewState("Initializing...", Color(0xFF2196F3), false)
    )

    val viewState: StateFlow<MonitorViewState> = _viewState

    init {
        viewModelScope.launch {
            networkMonitorController.status
                .map { state ->
                    val (text, color) = when (state) {
                        GlobalReachabilityState.FullyAvailable -> "Fully Online" to Color(
                            0xFF4CAF50
                        ) // Green
                        is GlobalReachabilityState.DegradedLatency -> "Slow: ${state.pingTimeMs}ms" to Color(
                            0xFFFF9800
                        ) // Orange
                        GlobalReachabilityState.ServerUnreachable -> "Server Offline/Unreachable" to Color(
                            0xFFF44336
                        ) // Red
                        GlobalReachabilityState.NoNetwork -> "Device Offline" to Color(
                            0xFF9E9E9E
                        ) // Grey
                        GlobalReachabilityState.InternetBlocked -> "Internet Blocked (Portal?)" to Color(
                            0xFFFFC107
                        ) // Yellow
                        GlobalReachabilityState.Checking -> "Checking Status..." to Color(
                            0xFF2196F3
                        ) // Blue
                    }

                    MonitorViewState(
                        statusText = text,
                        statusColor = color,
                        isSocketActive = isSocketConnected
                    )
                }
                .collect { newState ->
                    _viewState.value = newState
                }
        }
    }

    /**
     * Toggles the simulated Socket.IO connection.
     * CRITICAL: Calls the controller to pause/resume the background polling job.
     */
    fun toggleSocketConnection() {
        isSocketConnected = !isSocketConnected

        networkMonitorController.setHighPriorityActive(isSocketConnected)

        // 4. To update the UI, modify the value of the private _viewState.
        _viewState.value = _viewState.value.copy(isSocketActive = isSocketConnected)
    }

    /**
     * Optional: Function to force the app to reset the state and check again.
     * For demonstration only, production code relies on the Flow.
     */
    fun checkInternetNow() {
        if (!isSocketConnected) {
            networkMonitorController.setHighPriorityActive(false)
        }
    }
}
