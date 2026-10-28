package com.tuapp.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val nombre: String,
    val avatar: String = "",
    val bio: String = "",
    val ultima: Double = 0.0,
    @SerialName("en_linea") val enLinea: Boolean = false
)
