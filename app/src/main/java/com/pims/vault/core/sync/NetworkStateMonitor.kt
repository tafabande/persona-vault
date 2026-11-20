package com.pims.vault.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val networkState: Flow<NetworkState> = callbackFlow {
        if (connectivityManager == null) {
            trySend(NetworkState.OFFLINE)
            awaitClose {}
            return@callbackFlow
        }

        val isConnectedInitially = isCurrentlyConnected()
        trySend(if (isConnectedInitially) NetworkState.ONLINE else NetworkState.OFFLINE)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(if (hasInternet(capabilities)) NetworkState.ONLINE else NetworkState.OFFLINE)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.OFFLINE)
            }

            override fun onUnavailable() {
                trySend(NetworkState.OFFLINE)
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            trySend(NetworkState.OFFLINE)
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    fun isCurrentlyConnected(): Boolean {
        return try {
            if (connectivityManager == null) return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            hasInternet(capabilities)
        } catch (_: Exception) {
            false
        }
    }

    private fun hasInternet(capabilities: NetworkCapabilities): Boolean =
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
