package com.avikmakwana.netstat.domain.monitor

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The single configuration object for the entire network-monitor library.
 * The host app MUST provide an instance of this object via Hilt to override the defaults.
 */
data class NetworkMonitorConfig(
    /**
     * The mandatory, production-grade URL for the application's specific health check.
     * This MUST resolve to an endpoint that returns a 2xx status code when healthy.
     */
    val serverHealthCheckUrl: String = "https://www.google.com/generate_204",

    /**
     * The maximum acceptable latency (Round Trip Time) for the server check.
     * If the RTT exceeds this, the state transitions to GlobalReachabilityState.DegradedLatency.
     */
    val maxLatencyThreshold: Duration = 500.milliseconds,

    /**
     * The minimum time the monitoring loop waits between server health checks
     * when the system is FullyAvailable.
     */
    val pollingIntervalMinimum: Duration = 15.seconds,

    /**
     * The maximum time the monitoring loop waits between checks when the server is
     * UNREACHABLE (used for backoff). Used to prevent hammering a down server.
     */
    val pollingIntervalMaximum: Duration = 60.seconds,

    /**
     * The timeout for the explicit server health check operation.
     * If the server doesn't respond in this time, it's considered Unreachable.
     */
    val healthCheckTimeout: Duration = 3.seconds
)
