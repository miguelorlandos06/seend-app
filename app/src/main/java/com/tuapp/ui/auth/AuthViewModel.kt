package com.tuapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.data.ChatWebSocket
import com.tuapp.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class AuthUiState(
    val autenticado: Boolean = false,
    val nombre: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val conectado: Boolean = false
)

class AuthViewModel(
    private val ws: ChatWebSocket,
    private val store: SessionStore
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    private var escuchando = false

    fun iniciar() {
        if (escuchando) return
        escuchando = true
        viewModelScope.launch {
            ws.events.collect { m ->
                when (m["tipo"]?.jsonPrimitive?.content) {
                    "auth_ok" -> {
                        val token  = m["token"]!!.jsonPrimitive.content
                        val nombre = m["nombre"]!!.jsonPrimitive.content
                        store.guardar(token, nombre)
                        _ui.update { it.copy(autenticado = true, nombre = nombre, cargando = false, error = null) }
                        ws.entrar(token)
                    }
                    "sesion_valida" -> {
                        val nombre = m["nombre"]!!.jsonPrimitive.content
                        _ui.update { it.copy(autenticado = true, nombre = nombre, conectado = true) }
                        store.token()?.let { ws.entrar(it) }
                    }
                    "sesion_invalida" -> {
                        store.clear()
                        _ui.update { it.copy(autenticado = false, nombre = "", cargando = false, conectado = false) }
                    }
                    "error" -> {
                        _ui.update {
                            it.copy(
                                cargando = false,
                                error = m["texto"]?.jsonPrimitive?.content ?: "Error"
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            val token = store.token()
            if (token != null) conectarYValidar(token)
        }
    }

    private suspend fun conectarYValidar(token: String) {
        try {
            ws.conectar()
            _ui.update { it.copy(conectado = true) }
            ws.validarSesion(token)
        } catch (e: Exception) {
            _ui.update {
                it.copy(
                    conectado = false, cargando = false,
                    error = "No se pudo conectar: ${e.message}"
                )
            }
        }
    }

    fun login(nombre: String, pass: String) {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                if (!ws.connected.value) ws.conectar()
                _ui.update { it.copy(conectado = true) }
                ws.login(nombre.trim(), pass)
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = "No se pudo conectar: ${e.message}") }
            }
        }
    }

    fun registro(nombre: String, pass: String) {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                if (!ws.connected.value) ws.conectar()
                _ui.update { it.copy(conectado = true) }
                ws.registro(nombre.trim(), pass)
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = "No se pudo conectar: ${e.message}") }
            }
        }
    }

    fun limpiarError() = _ui.update { it.copy(error = null) }

    fun cerrarSesion() {
        viewModelScope.launch {
            store.clear()
            ws.cerrar()
            _ui.value = AuthUiState()
            escuchando = false
            iniciar()
        }
    }
}
