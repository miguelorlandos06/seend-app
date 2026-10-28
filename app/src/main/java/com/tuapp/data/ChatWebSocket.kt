package com.tuapp.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatWebSocket(
    private val url: String = Config.WS_URL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) {
    @Volatile private var ws: WebSocket? = null
    private val _events = MutableSharedFlow<JsonObject>(extraBufferCapacity = 128)
    val events: SharedFlow<JsonObject> = _events.asSharedFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    
    var onMensajeFallido: ((JsonObject) -> Unit)? = null // Callback para la cola

    suspend fun conectar(): Unit = suspendCancellableCoroutine { cont ->
        val req = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(w: WebSocket, r: Response) { _connected.value = true; if (cont.isActive) cont.resume(Unit) }
            override fun onMessage(w: WebSocket, text: String) {
                runCatching { Json.parseToJsonElement(text).jsonObject }.onSuccess { _events.tryEmit(it) }
            }
            override fun onFailure(w: WebSocket, t: Throwable, r: Response?) {
                _connected.value = false; if (cont.isActive) cont.resumeWithException(t)
            }
            override fun onClosed(w: WebSocket, code: Int, reason: String) {
                _connected.value = false; if (cont.isActive) cont.resumeWithException(Exception("Cerrado"))
            }
        }
        ws = client.newWebSocket(req, listener)
        cont.invokeOnCancellation { ws?.close(1000, "cancelado") }
    }

    fun cerrar() { ws?.close(1000, "bye"); ws = null; _connected.value = false }

    fun send(obj: JsonObject): Boolean {
        val actualWs = ws
        return if (actualWs != null && _connected.value) {
            val exito = actualWs.send(obj.toString())
            if (!exito) onMensajeFallido?.invoke(obj)
            exito
        } else {
            onMensajeFallido?.invoke(obj)
            false
        }
    }

    fun login(nombre: String, pass: String) = send(buildJsonObject { put("tipo", "login"); put("nombre", nombre); put("password", pass) })
    fun registro(nombre: String, pass: String) = send(buildJsonObject { put("tipo", "registro"); put("nombre", nombre); put("password", pass) })
    fun validarSesion(token: String) = send(buildJsonObject { put("tipo", "validar_sesion"); put("token", token) })
    fun entrar(token: String) = send(buildJsonObject { put("tipo", "entrar"); put("token", token) })
    fun pedirChats() = send(buildJsonObject { put("tipo", "pedir_chats") })
    fun pedirUsuarios() = send(buildJsonObject { put("tipo", "pedir_usuarios") })
    fun abrirChat(otro: String) = send(buildJsonObject { put("tipo", "abrir_chat"); put("otro", otro) })
    fun marcarLeido(otro: String, ids: List<String>) = send(buildJsonObject { put("tipo", "marcar_leido"); put("otro", otro); put("ids", JsonArray(ids.map { JsonPrimitive(it) })) })
    fun enviarMensaje(para: String, texto: String, responderA: String?) = send(buildJsonObject { put("tipo", "mensaje"); put("para", para); put("texto", texto); if (responderA != null) put("responder_a", responderA) })
    fun enviarMensajeConImagen(para: String, texto: String, imagenUrl: String, imagenTamano: String, imagenDim: String, blurhash: String = "") = send(buildJsonObject { put("tipo", "mensaje"); put("para", para); put("texto", texto); put("imagen_url", imagenUrl); put("imagen_tamano", imagenTamano); put("imagen_dim", imagenDim); if (blurhash.isNotBlank()) put("blurhash", blurhash) })
    fun escribiendo(para: String, activo: Boolean) = send(buildJsonObject { put("tipo", "escribiendo"); put("para", para); put("activo", activo) })
    fun actualizarPerfil(nombre: String, bio: String, avatarUrl: String?) = send(buildJsonObject { put("tipo", "actualizar_perfil"); put("nombre", nombre); put("bio", bio); if (avatarUrl != null) put("avatar_url", avatarUrl) })
    fun pedirPerfil(quien: String) = send(buildJsonObject { put("tipo", "pedir_perfil"); put("quien", quien) })
}
