package com.tuapp.ui.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class PerfilPublico(val nombre: String, val bio: String, val avatar: String, val enLinea: Boolean, val ultima: Double)

data class UiState(
    val autenticado: Boolean = false, val conectado: Boolean = false,
    val estadoConexion: EstadoConexion = EstadoConexion.Desconectado,
    val nombre: String = "", val bioPropio: String = "", val avatarPropio: String = "",
    val chats: List<ChatItem> = emptyList(), val usuarios: List<Usuario> = emptyList(),
    val mostrandoUsuarios: Boolean = false, val chatActual: String? = null,
    val mensajes: List<Msg> = emptyList(), val subiendo: List<Msg> = emptyList(),
    val escribiendo: Map<String, Boolean> = emptyMap(), val respondiendoA: Msg? = null,
    val error: String? = null, val cargando: Boolean = false, val guardandoPerfil: Boolean = false,
    val perfilOtro: PerfilPublico? = null, val mostrarMiPerfil: Boolean = false
)

class ChatViewModel(
    private val ws: ChatWebSocket, private val store: SessionStore,
    private val repo: ChatRepository, private val uploader: UploadRepository,
    private val monitor: NetworkMonitor, private val cola: ColaOfflineRepository,
    private val nombreInicial: String
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState(nombre = nombreInicial, autenticado = nombreInicial.isNotBlank()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var mensajesJob: Job? = null
    private var escribiendoJob: Job? = null
    private var escuchando = false
    private var ultimoEscribiendo = 0L

    private val reconexion = ReconexionManager(
        scope = viewModelScope, monitor = monitor,
        conectar = { ws.conectar(); store.token()?.let { ws.validarSesion(it) } },
        yaConectado = { ws.connected.value }
    )

    init {
        ws.onMensajeFallido = { obj ->
            if (obj["tipo"]?.jsonPrimitive?.content == "mensaje") {
                val para = obj["para"]?.jsonPrimitive?.content ?: ""
                val texto = obj["texto"]?.jsonPrimitive?.content ?: ""
                viewModelScope.launch { cola.encolar(para, texto, null, null, null, null) }
            }
        }
    }

    fun iniciar() {
        if (escuchando) return
        escuchando = true
        viewModelScope.launch { ws.events.collect(::manejarEvento) }
        viewModelScope.launch { reconexion.estado.collect { estado ->
            _ui.update { it.copy(estadoConexion = estado) }
            if (estado is EstadoConexion.Conectado) drenarCola()
        }}
        viewModelScope.launch { ws.connected.collect { if (!it) reconexion.onFallo() } }
        reconexion.iniciar()
        viewModelScope.launch {
            _ui.map { it.nombre }.distinctUntilChanged().collect { nombre ->
                if (nombre.isNotEmpty()) repo.chats(nombre).collect { chats -> _ui.update { it.copy(chats = chats) } }
            }
        }
        viewModelScope.launch { repo.usuarios().collect { usuarios -> _ui.update { it.copy(usuarios = usuarios) } } }
    }

    fun marcarAutenticado(nombre: String) { _ui.update { it.copy(autenticado = true, nombre = nombre) } }
    fun limpiarError() = _ui.update { it.copy(error = null) }

    private fun manejarEvento(m: JsonObject) {
        when (m["tipo"]?.jsonPrimitive?.content) {
            "sesion_valida" -> { _ui.update { it.copy(nombre = m["nombre"]?.jsonPrimitive?.content ?: "", autenticado = true) }; viewModelScope.launch { store.token()?.let { ws.entrar(it) } } }
            "conversacion" -> { viewModelScope.launch { repo.guardarMensajes(m["mensajes"]?.jsonArray?.map { parseMensaje(it.jsonObject) } ?: emptyList()) } }
            "mensaje" -> {
                val msg = parseMensaje(m["mensaje"]?.jsonObject ?: return)
                viewModelScope.launch {
                    if (m["eco"]?.jsonPrimitive?.booleanOrNull == true) {
                        val p = _ui.value.mensajes.find { it.id.startsWith("tmp_") && it.texto == msg.texto }
                        if (p != null) repo.borrarMensaje(p.id)
                    }
                    repo.guardarMensaje(msg)
                }
            }
            "estados" -> {
                val ids = m["ids"]?.jsonArray?.map { it.jsonPrimitive.content } ?: return
                val estado = m["estado"]?.jsonPrimitive?.content ?: return
                viewModelScope.launch {
                    if (ids.isEmpty() && estado == "visto") _ui.value.chatActual?.let { chat -> repo.actualizarEstados(_ui.value.mensajes.filter { it.de == chat }.map { it.id }, "visto") }
                    else repo.actualizarEstados(ids, estado)
                }
            }
            "escribiendo" -> {
                val de = m["de"]?.jsonPrimitive?.content ?: return
                val activo = m["activo"]?.jsonPrimitive?.boolean ?: false
                _ui.update { it.copy(escribiendo = it.escribiendo + (de to activo)) }
                if (activo) { escribiendoJob?.cancel(); escribiendoJob = viewModelScope.launch { delay(2000); _ui.update { it.copy(escribiendo = it.escribiendo + (de to false)) } } }
            }
            "perfil_actualizado" -> {
                val nombre = m["nombre"]?.jsonPrimitive?.content ?: return
                val bio = m["bio"]?.jsonPrimitive?.content ?: ""
                val avatar = m["avatar"]?.jsonPrimitive?.content ?: ""
                viewModelScope.launch { store.guardarNombre(nombre); repo.actualizarPerfilLocal(nombre, avatar, bio) }
                _ui.update { it.copy(nombre = nombre, bioPropio = bio, avatarPropio = avatar, guardandoPerfil = false, mostrarMiPerfil = false) }
            }
            "error" -> { _ui.update { it.copy(error = m["texto"]?.jsonPrimitive?.content ?: "Error", cargando = false, guardandoPerfil = false) } }
        }
    }

    fun enviarTexto(texto: String) {
        val chat = _ui.value.chatActual ?: return
        val t = texto.trim(); if (t.isEmpty()) return
        val resp = _ui.value.respondiendoA?.id
        val tmpId = "tmp_${UUID.randomUUID()}"
        val provisional = Msg(id = tmpId, de = _ui.value.nombre, para = chat, texto = t, hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), ts = System.currentTimeMillis() / 1000.0, estadoEnvio = if (ws.connected.value) EstadoEnvio.Enviando else EstadoEnvio.EnCola)
        viewModelScope.launch {
            repo.guardarMensaje(provisional)
            if (!ws.connected.value) cola.encolar(chat, t, null, null, null, resp)
            else ws.enviarMensaje(chat, t, resp)
        }
        _ui.update { it.copy(respondiendoA = null) }
        ws.escribiendo(chat, false)
    }

    fun enviarImagen(ctx: Context, uri: Uri, texto: String = "") {
        val chat = _ui.value.chatActual ?: return
        
        // Fix: Bloquear envío de imagen sin conexión
        if (!ws.connected.value) {
            _ui.update { it.copy(error = "Se requiere conexión para enviar imágenes") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            var size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            
            // Fix: Si el tamaño es -1, copiar a temporal para calcular el tamaño real
            if (size <= 0) {
                val tempFile = File.createTempFile("size_calc", ".tmp", ctx.cacheDir)
                ctx.contentResolver.openInputStream(uri)?.use { it.copyTo(tempFile.outputStream()) }
                size = tempFile.length()
                tempFile.delete()
            }
            
            if (size > 4 * 1024 * 1024) {
                withContext(Dispatchers.Main) { _ui.update { it.copy(error = "La imagen supera los 4 MB") } }
                return@launch
            }
            
            val dims = leerDimensiones(ctx, uri)
            val tmpId = "tmp_${UUID.randomUUID()}"
            val bitmap = try { ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 }) } } catch (e: Exception) { null }
            val hash = try { if (bitmap != null) com.tuapp.ui.components.BlurHash.encode(bitmap, 4, 4) else "" } catch(e: Exception) { "" }

            val provisional = Msg(id = tmpId, de = _ui.value.nombre, para = chat, texto = texto, hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), ts = System.currentTimeMillis() / 1000.0, subidaEstado = SubidaEstado.Pendiente, bytesTotales = size, dimensiones = dims)
            withContext(Dispatchers.Main) { _ui.update { it.copy(subiendo = it.subiendo + provisional) } }

            try {
                val token = store.token() ?: error("Sin token")
                val prep = uploader.prepare(token, "mensaje", chat, mime, size)
                withContext(Dispatchers.Main) { _ui.update { s -> s.copy(subiendo = s.subiendo.map { if (it.id == tmpId) it.copy(subidaEstado = SubidaEstado.Subiendo) else it }) } }
                uploader.upload(ctx, uri, prep, mime) { subido, total -> viewModelScope.launch(Dispatchers.Main) { _ui.update { s -> s.copy(subiendo = s.subiendo.map { if (it.id == tmpId) it.copy(progreso = if (total > 0) subido.toFloat() / total else 0f, bytesSubidos = subido, bytesTotales = total) else it }) } } }
                ws.enviarMensajeConImagen(chat, texto, prep.url_publica, fmtBytes(size), dims ?: "", hash)
                withContext(Dispatchers.Main) { _ui.update { s -> s.copy(subiendo = s.subiendo.filterNot { it.id == tmpId }) } }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _ui.update { s -> s.copy(subiendo = s.subiendo.map { if (it.id == tmpId) it.copy(subidaEstado = SubidaEstado.Fallida) else it }) } }
            }
        }
    }

    private fun drenarCola() {
        viewModelScope.launch(Dispatchers.IO) {
            val pendientes = cola.todos()
            for (p in pendientes) {
                if (p.imagenUri == null) { ws.enviarMensaje(p.para, p.texto, p.responderA); cola.borrar(p.idLocal) }
            }
        }
    }

    fun abrirChat(otro: String) {
        _ui.update { it.copy(chatActual = otro, mensajes = emptyList(), mostrandoUsuarios = false, respondiendoA = null) }
        mensajesJob?.cancel()
        mensajesJob = viewModelScope.launch { repo.mensajesDe(otro, _ui.value.nombre).collect { msgs -> _ui.update { it.copy(mensajes = msgs) } } }
        ws.abrirChat(otro); ws.pedirPerfil(otro)
        viewModelScope.launch { repo.marcarLeido(otro, _ui.value.nombre) }
    }

    fun cerrarChat() { mensajesJob?.cancel(); mensajesJob = null; _ui.update { it.copy(chatActual = null, mensajes = emptyList(), respondiendoA = null) } }
    fun mostrarUsuarios() { _ui.update { it.copy(mostrandoUsuarios = true) }; ws.pedirUsuarios() }
    fun volverAHome() = _ui.update { it.copy(mostrandoUsuarios = false) }
    fun responderA(m: Msg?) = _ui.update { it.copy(respondiendoA = m) }
    fun onTextoCambia(texto: String) { val chat = _ui.value.chatActual ?: return; val ahora = System.currentTimeMillis(); if (ahora - ultimoEscribiendo > 1000) { ws.escribiendo(chat, texto.isNotEmpty()); ultimoEscribiendo = ahora } }
    fun cambiarAvatar(ctx: Context, uri: Uri) { viewModelScope.launch(Dispatchers.IO) { try { val token = store.token() ?: error("Sin token"); val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"; val size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L; val prep = uploader.prepare(token, "avatar", null, mime, size); uploader.upload(ctx, uri, prep, mime) { _, _ -> }; withContext(Dispatchers.Main) { ws.actualizarPerfil(_ui.value.nombre, _ui.value.bioPropio, prep.url_publica) } } catch (e: Exception) { withContext(Dispatchers.Main) { _ui.update { it.copy(error = "Error al subir avatar") } } } } }
    fun leerDimensiones(ctx: Context, uri: Uri): String? = runCatching { val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }; ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }; if (opts.outWidth > 0) "${opts.outWidth} × ${opts.outHeight}" else null }.getOrNull()
    fun abrirMiPerfil() = _ui.update { it.copy(mostrarMiPerfil = true) }
    fun cerrarMiPerfil() = _ui.update { it.copy(mostrarMiPerfil = false) }
    fun guardarPerfil(nombre: String, bio: String) { _ui.update { it.copy(guardandoPerfil = true) }; ws.actualizarPerfil(nombre, bio, null) }
    fun pedirPerfil(quien: String) { ws.pedirPerfil(quien); _ui.update { it.copy(perfilOtro = PerfilPublico(quien, "", "", false, 0.0)) } }
    fun cerrarPerfilOtro() = _ui.update { it.copy(perfilOtro = null) }
    fun cerrarSesion() { viewModelScope.launch { store.clear(); ws.cerrar(); _ui.value = UiState(); escuchando = false } }

    private fun parseMensaje(o: JsonObject): Msg {
        val citaJson = o["cita"]?.jsonObject
        return Msg(o["id"]?.jsonPrimitive?.content ?: "", o["de"]?.jsonPrimitive?.content ?: "", o["para"]?.jsonPrimitive?.content ?: "", o["texto"]?.jsonPrimitive?.content ?: "", o["imagen_url"]?.jsonPrimitive?.content ?: "", o["imagen_tamano"]?.jsonPrimitive?.content ?: "", o["imagen_dim"]?.jsonPrimitive?.content ?: "", o["blurhash"]?.jsonPrimitive?.content ?: "", o["estado"]?.jsonPrimitive?.content ?: "enviado", o["hora"]?.jsonPrimitive?.content ?: "", o["ts"]?.jsonPrimitive?.doubleOrNull ?: 0.0, if (citaJson != null) Cita(citaJson["autor"]?.jsonPrimitive?.content ?: "", citaJson["texto"]?.jsonPrimitive?.content ?: "") else null)
    }
}
