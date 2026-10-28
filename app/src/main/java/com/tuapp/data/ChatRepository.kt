package com.tuapp.data

import com.tuapp.data.local.*
import com.tuapp.ui.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(private val db: AppDatabase) {

    fun mensajesDe(otro: String, yo: String): Flow<List<Msg>> =
        db.mensajes().observarConversacion(convKey(otro, yo)).map { list -> list.map { it.toMsg() } }.flowOn(Dispatchers.IO)

    fun chats(yo: String): Flow<List<ChatItem>> =
        combine(db.mensajes().observarTodosLosMensajes(), db.usuarios().observarUsuarios()) { todosMensajes, todosUsuarios ->
            val usuariosMap = todosUsuarios.associateBy { it.nombre }
            val agrupados = todosMensajes.groupBy { if (it.de == yo) it.para else it.de }
            agrupados.map { (otro, mensajes) ->
                val ultimo = mensajes.maxByOrNull { it.ts }!!
                val usuario = usuariosMap[otro]
                ChatItem(otro, Ultimo(ultimo.texto, ultimo.imagenUrl.isNotEmpty(), ultimo.hora, ultimo.ts, ultimo.de, ultimo.estado), 0, usuario?.enLinea ?: false, usuario?.avatar ?: "", usuario?.ultima ?: 0.0)
            }.sortedByDescending { it.ultimo.ts }
        }.flowOn(Dispatchers.IO)

    fun usuarios(): Flow<List<Usuario>> = db.usuarios().observarUsuarios().map { list -> list.map { it.toUsuario() } }.flowOn(Dispatchers.IO)

    suspend fun guardarMensaje(m: Msg) = withContext(Dispatchers.IO) { db.mensajes().insertar(m.toEntity()) }
    suspend fun borrarMensaje(id: String) = withContext(Dispatchers.IO) { db.mensajes().borrarPorId(id) }
    suspend fun guardarMensajes(msgs: List<Msg>) = withContext(Dispatchers.IO) { db.mensajes().insertarTodos(msgs.map { it.toEntity() }) }
    suspend fun actualizarEstados(ids: List<String>, estado: String) = withContext(Dispatchers.IO) { db.mensajes().actualizarEstados(ids, estado) }
    
    suspend fun reemplazarUsuarios(us: List<Usuario>) = withContext(Dispatchers.IO) {
        val existentes = us.map { it.toEntity() }
        db.usuarios().reemplazarTodos(existentes)
    }
    
    suspend fun actualizarUsuarioLocal(nombre: String, bio: String, avatar: String) = withContext(Dispatchers.IO) {
        db.usuarios().actualizarAvatar(nombre, avatar)
    }
    
    suspend fun marcarLeido(otro: String, yo: String) = withContext(Dispatchers.IO) { db.mensajes().marcarLeido(otro, yo) }

    companion object { fun convKey(a: String, b: String) = listOf(a, b).sorted().joinToString("|") }
}

fun Msg.toEntity() = MsgEntity(id, de, para, texto, imagenUrl, imagenTamano, imagenDim, blurhash, estado, hora, ts, cita?.autor, cita?.texto, ChatRepository.convKey(de, para))
fun MsgEntity.toMsg() = Msg(id, de, para, texto, imagenUrl, imagenTamano, imagenDim, blurhash, estado, hora, ts, if (citaAutor != null) Cita(citaAutor, citaTexto ?: "") else null, estadoEnvio = when (estado) { "visto" -> EstadoEnvio.Visto; "entregado" -> EstadoEnvio.Entregado; else -> EstadoEnvio.Enviado })
fun Usuario.toEntity() = UsuarioEntity(nombre, avatar, ultima, enLinea)
fun UsuarioEntity.toUsuario() = Usuario(nombre, avatar, "", ultima, enLinea)
