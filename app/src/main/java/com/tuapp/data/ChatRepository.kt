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
        db.mensajes().observarConversacion(convKey(otro, yo))
            .map { list -> list.map { it.toMsg() } }
            .flowOn(Dispatchers.IO)

    fun chats(yo: String): Flow<List<ChatItem>> =
        combine(
            db.mensajes().observarTodosLosMensajes(),
            db.usuarios().observarUsuarios()
        ) { todosMensajes, todosUsuarios ->
            val usuariosMap = todosUsuarios.associateBy { it.nombre }
            val agrupados = todosMensajes.groupBy { if (it.de == yo) it.para else it.de }
            agrupados.map { (otro, mensajes) ->
                val ultimo = mensajes.maxByOrNull { it.ts }!!
                val usuario = usuariosMap[otro]
                ChatItem(
                    otro = otro,
                    ultimo = Ultimo(
                        texto = ultimo.texto,
                        imagen = ultimo.imagenUrl.isNotEmpty(),
                        hora = ultimo.hora,
                        ts = ultimo.ts,
                        de = ultimo.de,
                        estado = ultimo.estado
                    ),
                    noLeidos = 0,
                    enLinea = usuario?.enLinea ?: false,
                    avatar = usuario?.avatar ?: "",
                    ultima = usuario?.ultima ?: 0.0
                )
            }.sortedByDescending { it.ultimo.ts }
        }.flowOn(Dispatchers.IO)

    fun usuarios(): Flow<List<Usuario>> =
        db.usuarios().observarUsuarios()
            .map { list -> list.map { it.toUsuario() } }
            .flowOn(Dispatchers.IO)

    suspend fun guardarMensaje(m: Msg) = withContext(Dispatchers.IO) {
        db.mensajes().insertar(m.toEntity())
    }

    suspend fun borrarMensaje(id: String) = withContext(Dispatchers.IO) {
        db.mensajes().borrarPorId(id)
    }

    suspend fun guardarMensajes(msgs: List<Msg>) = withContext(Dispatchers.IO) {
        db.mensajes().insertarTodos(msgs.map { it.toEntity() })
    }
    suspend fun actualizarEstados(ids: List<String>, estado: String) = withContext(Dispatchers.IO) {
        db.mensajes().actualizarEstados(ids, estado)
    }
    suspend fun reemplazarUsuarios(us: List<Usuario>) = withContext(Dispatchers.IO) {
        db.usuarios().reemplazarTodos(us.map { it.toEntity() })
    }
    suspend fun marcarLeido(otro: String, yo: String) = withContext(Dispatchers.IO) {
        db.mensajes().marcarLeido(otro, yo)
    }
    suspend fun actualizarUsuarioLocal(nombre: String, bio: String, avatar: String) = withContext(Dispatchers.IO) {
        val actual = db.usuarios().buscarPorNombre(nombre)
        db.usuarios().reemplazarTodos(listOf(
            UsuarioEntity(nombre, avatar, actual?.ultima ?: 0.0, actual?.enLinea ?: false)
        ))
    }

    companion object {
        fun convKey(a: String, b: String): String =
            listOf(a, b).sorted().joinToString("|")
    }
}

fun Msg.toEntity() = MsgEntity(
    id = id, de = de, para = para, texto = texto,
    imagenUrl = imagenUrl, imagenTamano = imagenTamano, imagenDim = imagenDim, blurhash = blurhash,
    estado = estado, hora = hora, ts = ts,
    citaAutor = cita?.autor, citaTexto = cita?.texto,
    convKey = ChatRepository.convKey(de, para)
)
fun MsgEntity.toMsg() = Msg(
    id = id, de = de, para = para, texto = texto,
    imagenUrl = imagenUrl, imagenTamano = imagenTamano, imagenDim = imagenDim, blurhash = blurhash,
    estado = estado, hora = hora, ts = ts,
    cita = if (citaAutor != null) Cita(citaAutor, citaTexto ?: "") else null,
    estadoEnvio = when (estado) {
        "visto" -> EstadoEnvio.Visto
        "entregado" -> EstadoEnvio.Entregado
        else -> EstadoEnvio.Enviado
    }
)
fun Usuario.toEntity() = UsuarioEntity(nombre, avatar, ultima, enLinea)
fun UsuarioEntity.toUsuario() = Usuario(nombre = nombre, avatar = avatar, bio = "", ultima = ultima, enLinea = enLinea)
