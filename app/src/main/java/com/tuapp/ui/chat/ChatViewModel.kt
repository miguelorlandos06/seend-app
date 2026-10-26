package com.tuapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.data.ChatRepository
import com.tuapp.data.ChatWebSocket
import com.tuapp.data.SessionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.Locale

data class UiState(
    val conectado: Boolean = false,
    val nombre: String = "",
    val chats: List<ChatItem> = emptyList(),
    val usuarios: List<Usuario> = emptyList(),
    val mostrandoUsuarios: Boolean = false,
    val chatActual: String? = null,
    val mensajes: List<Msg> = emptyList(),
    val escribiendo: Map<String, Boolean> = emptyMap(),
    val respondiendoA: Msg? = null,
    val error: String? = null,
    val cargando: Boolean = false,
    val enviando: Boolean = false
)

class ChatViewModel(
    private val ws: ChatWebSocket,
    private val store: SessionStore,
    private val repo: ChatRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var mensajesJob: Job? = null
    private var escribiendoJob: Job? = null
    private var escuchando = false

    fun iniciar() {
        if (escuchando) return
        escuchando = true

        // Escuchar eventos WS
        viewModelScope.launch {
            ws.events.collect(::manejarEvento)
        }

        // Escuchar estado de conexión
        viewModelScope.launch {
            ws.connected.collect { conectado ->
                _ui.update { it.copy(conectado = conectado) }
            }
        }

        // Conectar y validar sesión
        viewModelScope.launch {
            try {
                ws.conectar()
                val token = store.token()
                if (token != null) {
                    ws.validarSesion(token)
                } else {
                    _ui.update { it.copy(error = "Sesión no encontrada") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo conectar: ${e.message}") }
            }
        }

        // Observar Room
        viewModelScope.launch {
            repo.chats().collect { chats -> _ui.update { it.copy(chats = chats) } }
        }
        viewModelScope.launch {
            repo.usuarios().collect { usuarios -> _ui.update { it.copy(usuarios = usuarios) } }
        }
    }

    private fun manejarEvento(m: JsonObject) {
        when (m["tipo"]?.jsonPrimitive?.content) {

            "sesion_valida" -> {
                val nombre = m["nombre"]!!.jsonPrimitive.content
                _ui.update { it.copy(nombre = nombre) }
                viewModelScope.launch {
                    val token = store.token()
                    if (token != null) ws.entrar(token)
                }
            }

            "sesion_invalida" -> {
                viewModelScope.launch { store.clear() }
                _ui.update { it.copy(error = "Sesión inválida. Vuelve a iniciar sesión.") }
            }

            "chats" -> {
                val chats = parseChats(m["chats"]!!.jsonArray)
                viewModelScope.launch { repo.reemplazarChats(chats) }
            }

            "usuarios" -> {
                val usuarios = parseUsuarios(m["usuarios"]!!.jsonArray)
                viewModelScope.launch { repo.reemplazarUsuarios(usuarios) }
            }

            "conversacion" -> {
                val otro = m["otro"]!!.jsonPrimitive.content
                val mensajes = parseMensajes(m["mensajes"]!!.jsonArray)
                val chat = _ui.value.chatActual
                if (chat == otro) {
                    viewModelScope.launch {
                        repo.guardarMensajes(mensajes)
                        val ids = mensajes.filter { it.de == otro && it.estado != "visto" }.map { it.id }
                        if (ids.isNotEmpty()) ws.marcarLeido(otro, ids)
                    }
                }
            }

            "mensaje" -> {
                val msg = parseMensaje(m["mensaje"]!!.jsonObject)
                val eco = m["eco"]?.jsonPrimitive?.booleanOrNull == true
                viewModelScope.launch {
                    repo.guardarMensaje(msg)
                    val otro = if (msg.de == _ui.value.nombre) msg.para else msg.de
                    if (_ui.value.chatActual == otro && msg.de != _ui.value.nombre) {
                        ws.marcarLeido(otro, listOf(msg.id))
                    }
                }
                ws.pedirChats()
            }

            "estados" -> {
                val ids = m["ids"]!!.jsonArray.map { it.jsonPrimitive.content }
                val estado = m["estado"]!!.jsonPrimitive.content
                viewModelScope.launch {
                    repo.actualizarEstados(ids, estado)
                    ws.pedirChats()
                }
            }

            "escribiendo" -> {
                val de = m["de"]!!.jsonPrimitive.content
                val activo = m["activo"]!!.jsonPrimitive.boolean
                _ui.update { it.copy(escribiendo = it.escribiendo + (de to activo)) }
                if (activo) {
                    escribiendoJob?.cancel()
                    escribiendoJob = viewModelScope.launch {
                        delay(2000)
                        _ui.update { it.copy(escribiendo = it.escribiendo + (de to false)) }
                    }
                }
            }

            "presencia" -> {
                val usuario = m["usuario"]!!.jsonPrimitive.content
                val enLinea = m["en_linea"]!!.jsonPrimitive.boolean
                val ultima = m["ultima"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                viewModelScope.launch {
                    val chats = _ui.value.chats.map {
                        if (it.otro == usuario) it.copy(enLinea = enLinea, ultima = ultima) else it
                    }
                    val usuarios = _ui.value.usuarios.map {
                        if (it.nombre == usuario) it.copy(enLinea = enLinea, ultima = ultima) else it
                    }
                    repo.reemplazarChats(chats)
                    repo.reemplazarUsuarios(usuarios)
                }
            }

            "perfil_actualizado" -> {
                val nombre = m["nombre"]!!.jsonPrimitive.content
                viewModelScope.launch { store.guardarNombre(nombre) }
                _ui.update { it.copy(nombre = nombre) }
            }

            "perfil_actualizado_otro" -> {
                val nombre = m["nombre"]!!.jsonPrimitive.content
                val avatar = m["avatar"]?.jsonPrimitive?.content ?: ""
                viewModelScope.launch {
                    val chats = _ui.value.chats.map {
                        if (it.otro == nombre) it.copy(avatar = avatar) else it
                    }
                    val usuarios = _ui.value.usuarios.map {
                        if (it.nombre == nombre) it.copy(avatar = avatar) else it
                    }
                    repo.reemplazarChats(chats)
                    repo.reemplazarUsuarios(usuarios)
                }
            }

            "error" -> {
                val texto = m["texto"]?.jsonPrimitive?.content ?: "Error"
                _ui.update { it.copy(error = texto, cargando = false) }
            }
        }
    }

    fun abrirChat(otro: String) {
        _ui.update {
            it.copy(
                chatActual = otro,
                mensajes = emptyList(),
                mostrandoUsuarios = false,
                respondiendoA = null
            )
        }
        mensajesJob?.cancel()
        mensajesJob = viewModelScope.launch {
            repo.mensajesDe(otro, _ui.value.nombre).collect { msgs ->
                _ui.update { it.copy(mensajes = msgs) }
            }
        }
        ws.abrirChat(otro)
        viewModelScope.launch { repo.marcarLeido(otro) }
    }

    fun cerrarChat() {
        mensajesJob?.cancel()
        mensajesJob = null
        _ui.update {
            it.copy(chatActual = null, mensajes = emptyList(), respondiendoA = null)
        }
    }

    fun mostrarUsuarios() {
        _ui.update { it.copy(mostrandoUsuarios = true) }
        ws.pedirUsuarios()
    }

    fun volverAHome() {
        _ui.update { it.copy(mostrandoUsuarios = false) }
    }

    fun responderA(m: Msg?) = _ui.update { it.copy(respondiendoA = m) }

    fun enviarTexto(texto: String) {
        val chat = _ui.value.chatActual ?: return
        val textoLimpio = texto.trim()
        if (textoLimpio.isEmpty()) return
        val resp = _ui.value.respondiendoA?.id
        ws.enviarMensaje(chat, textoLimpio, resp)
        _ui.update { it.copy(respondiendoA = null) }
        ws.escribiendo(chat, false)
    }

    fun onTextoCambia(texto: String) {
        val chat = _ui.value.chatActual ?: return
        if (texto.isNotEmpty()) ws.escribiendo(chat, true)
        else ws.escribiendo(chat, false)
    }

    fun limpiarError() = _ui.update { it.copy(error = null) }

    fun cerrarSesion() {
        viewModelScope.launch {
            store.clear()
            ws.cerrar()
            _ui.value = UiState()
            escuchando = false
        }
    }

    // ============================================================
    // Parsers JSON
    // ============================================================
    private fun parseChats(arr: JsonArray): List<ChatItem> = arr.map { el ->
        val o = el.jsonObject
        val ult = o["ultimo"]!!.jsonObject
        ChatItem(
            otro = o["otro"]!!.jsonPrimitive.content,
            ultimo = Ultimo(
                texto = ult["texto"]?.jsonPrimitive?.content ?: "",
                imagen = ult["imagen"]?.jsonPrimitive?.boolean ?: false,
                hora = ult["hora"]?.jsonPrimitive?.content ?: "",
                ts = ult["ts"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                de = ult["de"]?.jsonPrimitive?.content ?: "",
                estado = ult["estado"]?.jsonPrimitive?.content ?: "enviado"
            ),
            noLeidos = o["no_leidos"]?.jsonPrimitive?.int ?: 0,
            enLinea = o["en_linea"]?.jsonPrimitive?.boolean ?: false,
            avatar = o["avatar"]?.jsonPrimitive?.content ?: "",
            ultima = o["ultima"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        )
    }

    private fun parseUsuarios(arr: JsonArray): List<Usuario> = arr.map { el ->
        val o = el.jsonObject
        Usuario(
            nombre = o["nombre"]!!.jsonPrimitive.content,
            avatar = o["avatar"]?.jsonPrimitive?.content ?: "",
            ultima = o["ultima"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            enLinea = o["en_linea"]?.jsonPrimitive?.boolean ?: false
        )
    }

    private fun parseMensajes(arr: JsonArray): List<Msg> = arr.map { parseMensaje(it.jsonObject) }

    private fun parseMensaje(o: JsonObject): Msg {
        val citaJson = o["cita"]?.jsonObject
        return Msg(
            id = o["id"]!!.jsonPrimitive.content,
            de = o["de"]!!.jsonPrimitive.content,
            para = o["para"]!!.jsonPrimitive.content,
            texto = o["texto"]?.jsonPrimitive?.content ?: "",
            imagenUrl = o["imagen_url"]?.jsonPrimitive?.content ?: "",
            imagenTamano = o["imagen_tamano"]?.jsonPrimitive?.content ?: "",
            imagenDim = o["imagen_dim"]?.jsonPrimitive?.content ?: "",
            estado = o["estado"]?.jsonPrimitive?.content ?: "enviado",
            hora = o["hora"]?.jsonPrimitive?.content ?: "",
            ts = o["ts"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            cita = if (citaJson != null) Cita(
                autor = citaJson["autor"]?.jsonPrimitive?.content ?: "",
                texto = citaJson["texto"]?.jsonPrimitive?.content ?: ""
            ) else null,
            estadoEnvio = when (o["estado"]?.jsonPrimitive?.content) {
                "visto" -> EstadoEnvio.Visto
                "entregado" -> EstadoEnvio.Entregado
                else -> EstadoEnvio.Enviado
            }
        )
    }
}
