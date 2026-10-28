package com.tuapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class UploadRepository(
    private val baseHttp: String = Config.BASE_HTTP,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    suspend fun prepare(token: String, tipoArchivo: String, para: String?, mime: String, bytes: Long): UploadPrepareResponse = withContext(Dispatchers.IO) {
        val body = ApiJson.encodeToString(UploadPrepareRequest.serializer(), UploadPrepareRequest(token, tipoArchivo, mime, bytes, para)).toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$baseHttp/upload/prepare").post(body).build()
        client.newCall(req).execute().use { r ->
            val txt = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw RuntimeException("prepare ${r.code}: $txt")
            ApiJson.decodeFromString(UploadPrepareResponse.serializer(), txt)
        }
    }

    suspend fun upload(ctx: Context, uri: Uri, prep: UploadPrepareResponse, mime: String, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val (stream, size) = openStreamSafe(ctx, uri)
        val body = ProgressRequestBody(stream, size, mime, onProgress)
        val urlFinal = if (prep.url_put.startsWith("http")) prep.url_put else "$baseHttp${prep.url_put}"
        val req = Request.Builder().url(urlFinal).put(body).header("Content-Type", mime).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw RuntimeException("upload ${r.code}: ${r.body?.string()}")
        }
    }

    // Arreglo del OOM: Si el tamaño es -1, copiamos a un archivo temporal en lugar de leer a RAM
    private fun openStreamSafe(ctx: Context, uri: Uri): Pair<java.io.InputStream, Long> {
        val inputStream = ctx.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el archivo")
        var size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        
        if (size <= 0) {
            val tempFile = File.createTempFile("upload_", ".tmp", ctx.cacheDir)
            tempFile.outputStream().use { out -> inputStream.copyTo(out) }
            return tempFile.inputStream() to tempFile.length()
        }
        return inputStream to size
    }
}
