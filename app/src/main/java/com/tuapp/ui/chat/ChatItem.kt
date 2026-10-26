package com.tuapp.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ultimo(
    val texto: String = "",
    val imagen: Boolean = false,
    val hora: String = "",
    val ts: Double = 0.0,
    val de: String = "",
    val estado: String = "enviado"
)

@Serializable
data class ChatItem(
    val otro: String,
    val ultimo: Ultimo,
    @SerialName("no_leidos") val noLeidos: Int = 0,
    @SerialName("en_linea") val enLinea: Boolean = false,
    val avatar: String = "",
    val ultima: Double = 0.0
)
