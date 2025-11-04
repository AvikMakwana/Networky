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
 */
internal interface OSNetworkMonitorRepository {
    val osNetworkStatus: Flow<NetworkReachability>
}

/**
 * Implementation that wraps the Android ConnectivityManager into a reactive Flow.
 * This is the production-grade way to handle network state changes on API 24+.
 */
@Singleton
internal class DefaultNetworkMonitorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : OSNetworkMonitorRepository {

    // IMPORTANT: connectivityManager is directly available to the inner callback object.
    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    override val osNetworkStatus: Flow<NetworkReachability> = callbackFlow {
        // We request a network with INTERNET capability. VALIDATION is checked in getCurrentStatus().
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {

            /**
             * CRITICAL FIX: Determines the network reachability status by checking the definitive
             * active network state. Visibility modifier removed to allow access from the trySend.
             */
            fun getCurrentStatus(): NetworkReachability {
                // Safely retrieve the active network from the outer class's property
                val activeNetwork = connectivityManager?.activeNetwork ?: return NetworkReachability.Disconnected
                val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

                return when {
                    caps == null -> {
                        NetworkReachability.Disconnected
                    }
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                        // The OS has validated this network has global internet access.
                        NetworkReachability.Available
                    }
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
                        // Connected locally, but OS validation failed (Ghost Connection / Captive Portal).
                        NetworkReachability.ConnectedButNoInternet
                    }
                    else -> {
                        NetworkReachability.Disconnected
                    }
                }
            }

            override fun onAvailable(network: Network) {
                trySend(getCurrentStatus())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentStatus())
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                // Capabilities changing is the most frequent way the VALIDATED status changes.
                trySend(getCurrentStatus())
            }
        }

        // Send initial status immediately upon subscription
        // The call is now valid because getCurrentStatus is no longer private.
        trySend(callback.getCurrentStatus())

        // Register the callback
        connectivityManager?.registerNetworkCallback(request, callback)

        // Cleanup on flow cancellation
        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

}
