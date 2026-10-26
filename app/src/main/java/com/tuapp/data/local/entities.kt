package com.tuapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mensajes")
data class MsgEntity(
    @PrimaryKey val id: String,
    val de: String,
    val para: String,
    val texto: String,
    val imagenUrl: String,
    val imagenTamano: String,
    val imagenDim: String,
    val estado: String,
    val hora: String,
    val ts: Double,
    val citaAutor: String?,
    val citaTexto: String?,
    val convKey: String
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val otro: String,
    val texto: String,
    val imagen: Boolean,
    val hora: String,
    val ts: Double,
    val de: String,
    val estado: String,
    val noLeidos: Int,
    val enLinea: Boolean,
    val avatar: String,
    val ultima: Double
)

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val nombre: String,
    val avatar: String,
    val ultima: Double,
    val enLinea: Boolean
)
