package com.avikmakwana.netstat.domain.monitor

/**
 * The definitive, sealed class representing the single source of truth for the entire
 * application's global operational status, combining OS network status and server health.
 *
 * This is the public facing API state that the host application collects from the Flow.
 */
sealed class GlobalReachabilityState {

    /** Initial state while the monitor is starting up and collecting data. */
    data object Checking : GlobalReachabilityState()

    /** The device has validated internet access AND our backend health check passed within the latency threshold. */
    data object FullyAvailable : GlobalReachabilityState()

    /** * Internet is available, but the RTT (ping time) to the server exceeds the configured threshold.
     * Features may be slow, but are still technically operational.
     */
    data class DegradedLatency(val pingTimeMs: Long) : GlobalReachabilityState()

    /** The device has no active Wi-Fi or cellular network connection (OS-level loss). */
    data object NoNetwork : GlobalReachabilityState()

    /** * Device is connected locally, but the OS cannot validate worldwide internet access
     * (e.g., captive portal, firewall, ghost connection).
     */
    data object InternetBlocked : GlobalReachabilityState()

    /** * Internet is reachable, but the configured backend server failed its health check
     * (e.g., HTTP 5xx error, timeout, or DNS failure).
     */
    data object ServerUnreachable : GlobalReachabilityState()
}
