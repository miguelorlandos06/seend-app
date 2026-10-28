package com.tuapp.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Cita(val autor: String, val texto: String)

enum class EstadoEnvio { EnCola, Enviando, Enviado, Entregado, Visto, Error }
enum class SubidaEstado { Pendiente, Subiendo, Completada, Fallida }

@Serializable
data class Msg(
    val id: String, val de: String, val para: String,
    val texto: String = "",
    @SerialName("imagen_url") val imagenUrl: String = "",
    @SerialName("imagen_tamano") val imagenTamano: String = "",
    @SerialName("imagen_dim") val imagenDim: String = "",
    val blurhash: String = "",
    val estado: String = "enviado",
    val hora: String = "", val ts: Double = 0.0,
    val cita: Cita? = null,
    val subidaEstado: SubidaEstado? = null,
    val progreso: Float = 0f,
    val bytesSubidos: Long = 0, val bytesTotales: Long = 0,
    val dimensiones: String? = null,
    val estadoEnvio: EstadoEnvio = EstadoEnvio.Enviado
)
