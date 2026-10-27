package com.tuapp.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

sealed class EstadoConexion {
    object Conectado : EstadoConexion()
    object Conectando : EstadoConexion()
    object Desconectado : EstadoConexion()
    data class Error(val motivo: String) : EstadoConexion()
}

class ReconexionManager(
    private val scope: CoroutineScope,
    private val monitor: NetworkMonitor,
    private val conectar: suspend () -> Unit,
    private val yaConectado: () -> Boolean
) {
    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconectado)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    private var job: Job? = null
    private var intentos = 0

    init {
        scope.launch {
            monitor.estado.collect { red ->
                if (red.disponible && red.validada && _estado.value != EstadoConexion.Conectado) {
                    forzarReconexion()
                }
            }
        }
    }

    fun iniciar() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                if (yaConectado()) { _estado.value = EstadoConexion.Conectado; return@launch }
                _estado.value = EstadoConexion.Conectando
                try {
                    conectar()
                    intentos = 0
                    _estado.value = EstadoConexion.Conectado
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    intentos++
                    _estado.value = EstadoConexion.Error(e.message ?: "sin conexión")
                    delay(calcularEspera(intentos))
                }
            }
        }
    }

    fun forzarReconexion() {
        intentos = 0
        job?.cancel()
        iniciar()
    }

    fun onFallo() {
        _estado.value = EstadoConexion.Desconectado
        iniciar()
    }

    private fun calcularEspera(intento: Int): Long {
        val base = (1000L * (1L shl (intento - 1).coerceAtMost(5))).coerceAtMost(30000L)
        val jitter = (base * 0.2 * (Random.nextDouble() * 2 - 1)).toLong()
        return (base + jitter).coerceAtLeast(500L)
    }
}
