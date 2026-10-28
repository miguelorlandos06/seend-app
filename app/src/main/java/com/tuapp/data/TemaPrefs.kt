package com.tuapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.temaDs by preferencesDataStore("tema")
private val KEY_OSCURO = booleanPreferencesKey("oscuro")

class TemaPrefs(private val ctx: Context) {
    // Devuelve null si el usuario nunca ha tocado el interruptor.
    // MainActivity decidirá usar isSystemInDarkTheme() en ese caso.
    val oscuro: Flow<Boolean?> = ctx.temaDs.data.map { it[KEY_OSCURO] }
    suspend fun set(oscuro: Boolean) { ctx.temaDs.edit { it[KEY_OSCURO] = oscuro } }
}
