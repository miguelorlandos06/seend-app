package com.tuapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UploadPrepareRequest(
    val token: String,
    val tipo_archivo: String,
    val mime: String = "image/jpeg",
    val bytes: Long,
    val para: String? = null
)

@Serializable
data class UploadPrepareResponse(
    val upload_id: String,
    val url_put: String,
    val url_put_directa: String? = null,
    val url_publica: String,
    val msg_id: String = "",
    val expira_en: Int = 120,
    val error: String? = null
)

val ApiJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
