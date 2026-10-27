
package com.tuapp.data

import com.tuapp.data.local.PendienteDao
import com.tuapp.data.local.PendienteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ColaOfflineRepository(private val dao: PendienteDao) {
    suspend fun encolar(para: String, texto: String, imagenUri: String?, imagenDim: String?, imagenTamano: String?, responderA: String?) = withContext(Dispatchers.IO) {
        dao.encolar(PendienteEntity(
            idLocal = "pend_${UUID.randomUUID()}",
            para = para, texto = texto, imagenUri = imagenUri, imagenDim = imagenDim,
            imagenTamano = imagenTamano, responderA = responderA,
            ts = System.currentTimeMillis() / 1000.0
        ))
    }
    suspend fun borrar(id: String) = withContext(Dispatchers.IO) { dao.borrar(id) }
    suspend fun todos() = withContext(Dispatchers.IO) { dao.todos() }
}
