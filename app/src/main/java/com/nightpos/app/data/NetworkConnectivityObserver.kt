package com.nightpos.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Emits the current "do we have usable internet" state as a cold [Flow], backed by
 * [ConnectivityManager.NetworkCallback]. Used to drive the offline screen and the
 * "auto reconnect" behaviour without polling.
 */
class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(hasInternet())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(hasInternet())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        // The default NetworkRequest capability set requires NET_CAPABILITY_INTERNET
        // (validated public internet) and NET_CAPABILITY_NOT_VPN, so callbacks would
        // never fire for a WireGuard tunnel that only routes to a private Odoo
        // server. Remove both so we observe any active network — hasInternet()
        // re-checks the actual active network's transports either way.
        val request = NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // Emit the initial state immediately so collectors don't wait for a change event.
        trySend(hasInternet())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Don't gate on NET_CAPABILITY_INTERNET/VALIDATED: those reflect whether
        // Android's NetworkMonitor could reach a public connectivity-check endpoint,
        // which fails on networks that only route to a private Odoo server (e.g. over
        // a WireGuard tunnel with no general internet access). Any connected
        // transport is treated as "online" — GeckoView's own page load determines
        // real reachability to the configured POS server.
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
