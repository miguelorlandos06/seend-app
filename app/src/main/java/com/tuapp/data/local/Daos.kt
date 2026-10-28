package com.tuapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeDao {
    @Query("SELECT * FROM mensajes WHERE convKey = :convKey ORDER BY ts ASC")
    fun observarConversacion(convKey: String): Flow<List<MsgEntity>>

    @Query("""
        SELECT m.* FROM mensajes m
        INNER JOIN (
            SELECT convKey, MAX(ts) as max_ts FROM mensajes
            WHERE de = :yo OR para = :yo
            GROUP BY convKey
        ) latest ON m.convKey = latest.convKey AND m.ts = latest.max_ts
    """)
    fun observarUltimosMensajes(yo: String): Flow<List<MsgEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(msgs: List<MsgEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(msg: MsgEntity)

    @Query("UPDATE mensajes SET estado = :estado WHERE id IN (:ids)")
    suspend fun actualizarEstados(ids: List<String>, estado: String)

    @Query("UPDATE mensajes SET estado = 'visto' WHERE de = :otro AND para = :yo")
    suspend fun marcarLeido(otro: String, yo: String)

    @Query("DELETE FROM mensajes WHERE id = :id")
    suspend fun borrarPorId(id: String)
}

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY enLinea DESC, nombre ASC")
    fun observarUsuarios(): Flow<List<UsuarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun reemplazarTodos(usuarios: List<UsuarioEntity>)

    @Query("UPDATE usuarios SET avatar = :avatar, bio = :bio WHERE nombre = :nombre")
    suspend fun actualizarPerfilLocal(nombre: String, avatar: String, bio: String)

    @Query("SELECT * FROM usuarios WHERE nombre = :nombre")
    suspend fun buscarPorNombre(nombre: String): UsuarioEntity?
}

@Dao
interface PendienteDao {
    @Query("SELECT * FROM pendientes ORDER BY ts ASC")
    suspend fun todos(): List<PendienteEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun encolar(p: PendienteEntity)
    @Query("DELETE FROM pendientes WHERE idLocal = :id")
    suspend fun borrar(id: String)
}
