package com.avikmakwana.netstat.domain.monitor

import kotlinx.coroutines.flow.Flow

/**
 * MANDATE: The public interface for the host app to control the monitoring behavior
 * and read the status. This is the main API for the host application.
 */
interface NetworkMonitorController {
    /** The single, reactive stream of the application's global status. */
    val status: Flow<GlobalReachabilityState>

    /**
     * Signals to the module that a high-priority, real-time connection (like Socket.IO) is active.
     *
     * If isActive=true (or connecting/reconnecting), the background polling is suspended to save battery.
     * If isActive=false, the background polling is immediately resumed (recovery check).
     */
    fun setHighPriorityActive(isActive: Boolean)
}
