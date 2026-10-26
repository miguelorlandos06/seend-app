package com.tuapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class UploadRepository(
    private val baseHttp: String = Config.BASE_HTTP,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    suspend fun prepare(
        token: String,
        tipoArchivo: String,
        para: String?,
        mime: String,
        bytes: Long
    ): UploadPrepareResponse = withContext(Dispatchers.IO) {
        val body = ApiJson
            .encodeToString(
                UploadPrepareRequest.serializer(),
                UploadPrepareRequest(token, tipoArchivo, mime, bytes, para)
            )
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$baseHttp/upload/prepare").post(body).build()
        client.newCall(req).execute().use { r ->
            val txt = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw RuntimeException("prepare ${r.code}: $txt")
            ApiJson.decodeFromString(UploadPrepareResponse.serializer(), txt)
        }
    }

    suspend fun upload(
        ctx: Context,
        uri: Uri,
        prep: UploadPrepareResponse,
        mime: String,
        onProgress: (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val (stream, size) = openStream(ctx, uri)
        val body = ProgressRequestBody(stream, size, mime, onProgress)

        // Preferir url_put_directa (MinIO directo) → sin proxy, sin latencia extra
        // Si no existe, usar url_put (relativa) → prefijar con baseHttp (proxy Render)
        val urlCruda = prep.url_put_directa?.takeIf { it.isNotBlank() } ?: prep.url_put
        val urlFinal = if (urlCruda.startsWith("http")) urlCruda
                       else "$baseHttp$urlCruda"

        val req = Request.Builder()
            .url(urlFinal)
            .put(body)
            .header("Content-Type", mime)
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw RuntimeException("upload ${r.code}: ${r.body?.string()}")
        }
    }

    private fun openStream(ctx: Context, uri: Uri): Pair<java.io.InputStream, Long> {
        val stream = ctx.contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir el archivo")
        val size = ctx.contentResolver
            .openAssetFileDescriptor(uri, "r")
            ?.use { it.length }
            ?: 0L
        return stream to size
    }
}
