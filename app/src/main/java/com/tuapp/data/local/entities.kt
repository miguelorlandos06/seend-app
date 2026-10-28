package com.tuapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mensajes")
data class MsgEntity(
    @PrimaryKey val id: String,
    val de: String, val para: String,
    val texto: String,
    val imagenUrl: String, val imagenTamano: String, val imagenDim: String, val blurhash: String,
    val estado: String, val hora: String, val ts: Double,
    val citaAutor: String?, val citaTexto: String?,
    val convKey: String
)

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val nombre: String,
    val avatar: String,
    val bio: String,
    val ultima: Double,
    val enLinea: Boolean
)

@Entity(tableName = "pendientes")
data class PendienteEntity(
    @PrimaryKey val idLocal: String,
    val para: String,
    val texto: String,
    val imagenUri: String?,
    val imagenDim: String?,
    val imagenTamano: String?,
    val responderA: String?,
    val ts: Double,
    val intentos: Int = 0
)
