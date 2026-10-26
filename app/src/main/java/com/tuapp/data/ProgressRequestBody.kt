package com.tuapp.data

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

class ProgressRequestBody(
    private val input: InputStream,
    private val totalBytes: Long,
    private val mime: String,
    private val onProgress: (enviado: Long, total: Long) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = MediaType.parse(mime)
    override fun contentLength(): Long = totalBytes
    override fun writeTo(sink: BufferedSink) {
        val buf = ByteArray(64 * 1024)
        var enviado = 0L
        var last = 0L
        try {
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                sink.write(buf, 0, n)
                enviado += n
                val ahora = System.currentTimeMillis()
                if (ahora - last >= 80 || enviado == totalBytes) {
                    last = ahora
                    onProgress(enviado, totalBytes)
                }
            }
        } finally {
            runCatching { input.close() }
            onProgress(enviado, totalBytes)
        }
    }
}
