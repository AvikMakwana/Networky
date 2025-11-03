package com.avikmakwana.netstat.domain.usecases

import com.avikmakwana.netstat.di.IODispatcher
import com.avikmakwana.netstat.domain.monitor.NetworkMonitorConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MANDATE: The pure domain contract for checking server health and measuring latency.
 */
interface CheckServerHealthUseCase {
    /**
     * Executes a lightweight HEAD request to the configured health check URL.
     * @return The measured Round Trip Time (RTT) in milliseconds, or 0 if a definitive error occurred.
     */
    suspend operator fun invoke(): Long
}

/**
 * Implementation of the server health check, enforcing Thread Confinement.
 * It measures RTT and reports 0 on failure for consistent error handling by the [GlobalStatusMonitor].
 */
@Singleton
class CheckServerHealthUseCaseImpl @Inject constructor(
    private val config: NetworkMonitorConfig,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : CheckServerHealthUseCase {

    override suspend operator fun invoke(): Long = withContext(ioDispatcher) {
        val url = URL(config.serverHealthCheckUrl)
        var connection: HttpURLConnection? = null
        val startTime = System.currentTimeMillis()

        try {
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = config.healthCheckTimeout.inWholeMilliseconds.toInt()
            connection.readTimeout = config.healthCheckTimeout.inWholeMilliseconds.toInt()

            connection.connect()

            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime

            // If we get a 2xx response, the server is considered reachable/healthy.
            val isSuccessful = connection.responseCode in 200..299

            if (isSuccessful) {
                return@withContext latency
            } else {
                // Failure: Server responded, but with an error status (e.g., 500, 404).
                // Report 0 to signal an error state.
                return@withContext 0L
            }
        } catch (e: Exception) {
            // Failure: IOException, timeout, or DNS failure (Server Unreachable).
            return@withContext 0L
        } finally {
            connection?.disconnect()
        }
    }
}
