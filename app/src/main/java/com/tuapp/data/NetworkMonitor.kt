package com.tuapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class EstadoRed(val disponible: Boolean, val validada: Boolean)

class NetworkMonitor(private val ctx: Context) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val estado: Flow<EstadoRed> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(leerEstado()) }
            override fun onLost(network: Network) { trySend(leerEstado()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(leerEstado()) }
        }
        val req = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, callback)
        trySend(leerEstado())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun leerEstado(): EstadoRed {
        val activa = cm.activeNetwork ?: return EstadoRed(false, false)
        val caps = cm.getNetworkCapabilities(activa) ?: return EstadoRed(false, false)
        val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validada = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return EstadoRed(internet, validada)
    }
}
