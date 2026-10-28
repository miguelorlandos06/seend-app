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
    val error: String? = null
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
                        val token  = m["token"]?.jsonPrimitive?.content ?: return@collect
                        val nombre = m["nombre"]?.jsonPrimitive?.content ?: return@collect
                        store.guardar(token, nombre)
                        _ui.update { it.copy(autenticado = true, nombre = nombre, cargando = false, error = null) }
                        ws.entrar(token)
                    }
                    "error" -> {
                        _ui.update { it.copy(cargando = false, error = m["texto"]?.jsonPrimitive?.content ?: "Error") }
                    }
                }
            }
        }
    }

    fun login(nombre: String, pass: String) {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                if (!ws.connected.value) ws.conectar()
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
                ws.registro(nombre.trim(), pass)
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = "No se pudo conectar: ${e.message}") }
            }
        }
    }

    fun mostrarError(t: String) = _ui.update { it.copy(error = t, cargando = false) }
    fun limpiarError() = _ui.update { it.copy(error = null) }
}
