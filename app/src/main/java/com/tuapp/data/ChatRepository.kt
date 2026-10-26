package com.tuapp.data

import com.tuapp.data.local.*
import com.tuapp.ui.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(private val db: AppDatabase) {

    fun mensajesDe(otro: String, yo: String): Flow<List<Msg>> =
        db.mensajes().observarConversacion(convKey(otro, yo))
            .map { list -> list.map { it.toMsg() } }
            .flowOn(Dispatchers.IO)

    fun chats(): Flow<List<ChatItem>> =
        db.chats().observarChats()
            .map { list -> list.map { it.toChatItem() } }
            .flowOn(Dispatchers.IO)

    fun usuarios(): Flow<List<Usuario>> =
        db.usuarios().observarUsuarios()
            .map { list -> list.map { it.toUsuario() } }
            .flowOn(Dispatchers.IO)

    suspend fun guardarMensaje(m: Msg) = withContext(Dispatchers.IO) {
        db.mensajes().insertar(m.toEntity())
    }
    suspend fun guardarMensajes(msgs: List<Msg>) = withContext(Dispatchers.IO) {
        db.mensajes().insertarTodos(msgs.map { it.toEntity() })
    }
    suspend fun actualizarEstados(ids: List<String>, estado: String) = withContext(Dispatchers.IO) {
        db.mensajes().actualizarEstados(ids, estado)
    }
    suspend fun reemplazarChats(chats: List<ChatItem>) = withContext(Dispatchers.IO) {
        db.chats().reemplazarTodos(chats.map { it.toEntity() })
    }
    suspend fun reemplazarUsuarios(us: List<Usuario>) = withContext(Dispatchers.IO) {
        db.usuarios().reemplazarTodos(us.map { it.toEntity() })
    }
    suspend fun marcarLeido(otro: String) = withContext(Dispatchers.IO) {
        db.chats().marcarLeido(otro)
    }

    companion object {
        fun convKey(a: String, b: String): String =
            listOf(a, b).sorted().joinToString("|")
    }
}

// ---------- mappers ----------
fun Msg.toEntity() = MsgEntity(
    id = id, de = de, para = para, texto = texto,
    imagenUrl = imagenUrl, imagenTamano = imagenTamano, imagenDim = imagenDim,
    estado = estado, hora = hora, ts = ts,
    citaAutor = cita?.autor, citaTexto = cita?.texto,
    convKey = ChatRepository.convKey(de, para)
)

fun MsgEntity.toMsg() = Msg(
    id = id, de = de, para = para, texto = texto,
    imagenUrl = imagenUrl, imagenTamano = imagenTamano, imagenDim = imagenDim,
    estado = estado, hora = hora, ts = ts,
    cita = if (citaAutor != null) Cita(citaAutor, citaTexto ?: "") else null,
    estadoEnvio = when (estado) {
        "visto" -> EstadoEnvio.Visto
        "entregado" -> EstadoEnvio.Entregado
        else -> EstadoEnvio.Enviado
    }
)

fun ChatItem.toEntity() = ChatEntity(
    otro = otro, texto = ultimo.texto, imagen = ultimo.imagen, hora = ultimo.hora,
    ts = ultimo.ts, de = ultimo.de, estado = ultimo.estado, noLeidos = noLeidos,
    enLinea = enLinea, avatar = avatar, ultima = ultima
)

fun ChatEntity.toChatItem() = ChatItem(
    otro = otro,
    ultimo = Ultimo(texto, imagen, hora, ts, de, estado),
    noLeidos = noLeidos, enLinea = enLinea, avatar = avatar, ultima = ultima
)

fun Usuario.toEntity() = UsuarioEntity(nombre, avatar, ultima, enLinea)
fun UsuarioEntity.toUsuario() = Usuario(nombre, avatar, ultima, enLinea)
