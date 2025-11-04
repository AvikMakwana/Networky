package com.avikmakwana.networky.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avikmakwana.netstat.domain.monitor.GlobalReachabilityState
import com.avikmakwana.netstat.domain.monitor.NetworkMonitorController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Key used to store and retrieve the critical 'isSocketActive' state across process death.
 * MANDATE: All critical UI state MUST be managed by SavedStateHandle.
 */
private const val KEY_IS_SOCKET_ACTIVE = "key_is_socket_active"

/**
 * Represents the simplified UI state derived from the complex GlobalReachabilityState.
 * CRITICAL: This is the presentation layer's definitive state object.
 */
data class MonitorViewState(
    val statusText: String,
    val statusColor: Color,
    val isSocketActive: Boolean,
    val isChecking: Boolean // Added for better UI control
)

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val networkMonitorController: NetworkMonitorController,
    // CRITICAL: SavedStateHandle ensures state survives Process Death.
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 1. SavedStateHandle as the Source of Truth for mutable state.
    // The key is stored as a StateFlow for reactive reading.
    private val isSocketActiveFlow: StateFlow<Boolean> = savedStateHandle.getStateFlow(
        key = KEY_IS_SOCKET_ACTIVE,
        initialValue = false
    )

    // The host app ViewModel MUST map the library's state and its own state into one ViewState.
    val viewState: StateFlow<MonitorViewState> = networkMonitorController.status
        .combine(isSocketActiveFlow) { netState, isSocketActive ->
            val (text, color, isChecking) = when (netState) {
                GlobalReachabilityState.FullyAvailable -> Triple(
                    "Fully Online", Color(0xFF4CAF50), false
                ) // Green
                is GlobalReachabilityState.DegradedLatency -> Triple(
                    "Slow: ${netState.pingTimeMs}ms", Color(0xFFFF9800), false
                ) // Orange
                GlobalReachabilityState.ServerUnreachable -> Triple(
                    "Server Offline/Unreachable", Color(0xFFF44336), false
                ) // Red
                GlobalReachabilityState.NoNetwork -> Triple(
                    "Device Offline", Color(0xFF9E9E9E), false
                ) // Grey
                GlobalReachabilityState.InternetBlocked -> Triple(
                    "Internet Blocked (Portal?)", Color(0xFFFFC107), false
                ) // Yellow
                GlobalReachabilityState.Checking -> Triple(
                    "Checking Status...", Color(0xFF2196F3), true
                ) // Blue
            }

            MonitorViewState(
                statusText = text,
                statusColor = color,
                isSocketActive = isSocketActive,
                isChecking = isChecking
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonitorViewState("Initializing...", Color(0xFF2196F3), false, true)
        )

    init {
        // 2. Set the initial state of the library's controller based on the persisted state.
        // This ensures Process Death recovery immediately sets the correct background behavior.
        viewModelScope.launch {
            isSocketActiveFlow.collect { isActive ->
                networkMonitorController.setHighPriorityActive(isActive)
            }
        }
    }

    /**
     * Toggles the simulated Socket.IO connection.
     * CRITICAL: The SavedStateHandle is the source of truth for this state.
     */
    fun toggleSocketConnection() {
        val currentActive = isSocketActiveFlow.value
        val newActive = !currentActive

        // 3. Update the SavedStateHandle. This update is automatically persisted AND
        // updates the isSocketActiveFlow, which triggers the viewState to recalculate
        // (UDF enforced).
        savedStateHandle[KEY_IS_SOCKET_ACTIVE] = newActive

        // The logic to call setHighPriorityActive(isActive) is now handled reactively
        // in the init block's isSocketActiveFlow.collect { ... }
    }

    /**
     * Optional: Function to force the app to reset the state and check again.
     * This is an acceptable pattern if the feature is not active.
     */
    fun checkInternetNow() {
        if (!isSocketActiveFlow.value) {
            // By setting high priority to false, we force GlobalStatusMonitor to restart the poll loop.
            networkMonitorController.setHighPriorityActive(false)
        }
    }
}
