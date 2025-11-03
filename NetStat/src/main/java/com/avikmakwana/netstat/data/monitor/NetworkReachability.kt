package com.avikmakwana.netstat.data.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Internal sealed class to represent the OS-level network availability.
 * This is similar to GlobalReachabilityState but only reflects the low-level Android SDK view.
 */
internal sealed class NetworkReachability {
    data object Available : NetworkReachability()
    data object ConnectedButNoInternet : NetworkReachability()
    data object Disconnected : NetworkReachability()
}

/**
 * Defines the contract for the Data layer to report OS-level network state.
 * This is an internal contract, as only GlobalStatusMonitor should consume it.
 */
internal interface OSNetworkMonitorRepository {
    val osNetworkStatus: Flow<NetworkReachability>
}

/**
 * Implementation that wraps the Android ConnectivityManager into a reactive Flow.
 * This is the production-grade way to handle network state changes.
 */
@Singleton
internal class DefaultNetworkMonitorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : OSNetworkMonitorRepository {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    override val osNetworkStatus: Flow<NetworkReachability> = callbackFlow {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            private val availableNetworks = mutableSetOf<Network>()

            private fun updateAndSendStatus() {
                val hasValidatedInternet = availableNetworks.any { network ->
                    connectivityManager?.getNetworkCapabilities(network)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                }

                val status = when {
                    hasValidatedInternet -> NetworkReachability.Available
                    availableNetworks.isNotEmpty() -> NetworkReachability.ConnectedButNoInternet
                    else -> NetworkReachability.Disconnected
                }
                trySend(status)
            }

            override fun onAvailable(network: Network) {
                availableNetworks.add(network)
                updateAndSendStatus()
            }

            override fun onLost(network: Network) {
                availableNetworks.remove(network)
                updateAndSendStatus()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                updateAndSendStatus()
            }
        }

        val initialStatus = connectivityManager?.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { capabilities ->
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    NetworkReachability.Available
                } else {
                    NetworkReachability.ConnectedButNoInternet
                }
            }
        } ?: NetworkReachability.Disconnected
        trySend(initialStatus)

        connectivityManager?.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

}
