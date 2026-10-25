package com.tuapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sesionDs by preferencesDataStore("sesion")
private val KEY_TOKEN  = stringPreferencesKey("token")
private val KEY_NOMBRE = stringPreferencesKey("nombre")

class SessionStore(private val ctx: Context) {

    val tokenFlow: Flow<String?> = ctx.sesionDs.data.map { it[KEY_TOKEN] }
    val nombreFlow: Flow<String?> = ctx.sesionDs.data.map { it[KEY_NOMBRE] }

    suspend fun token(): String? = ctx.sesionDs.data.first()[KEY_TOKEN]
    suspend fun nombre(): String? = ctx.sesionDs.data.first()[KEY_NOMBRE]

    suspend fun guardar(token: String, nombre: String) {
        ctx.sesionDs.edit { it[KEY_TOKEN] = token; it[KEY_NOMBRE] = nombre }
    }
    suspend fun guardarNombre(nombre: String) {
        ctx.sesionDs.edit { it[KEY_NOMBRE] = nombre }
    }
    suspend fun clear() { ctx.sesionDs.edit { it.clear() } }
}
