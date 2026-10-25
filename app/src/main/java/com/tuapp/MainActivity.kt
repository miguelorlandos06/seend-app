package com.tuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.tuapp.ui.auth.AuthScreen
import com.tuapp.ui.theme.AppTheme
import com.tuapp.ui.theme.Texto
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            AppTheme {
                var nombre by rememberSaveable { mutableStateOf("") }
                var autenticado by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val t = runBlocking { app.sessionStore.token() }
                    if (t != null) {
                        nombre = runBlocking { app.sessionStore.nombre() } ?: ""
                        autenticado = true
                    }
                }

                if (!autenticado) {
                    AuthScreen(
                        ws = app.ws,
                        store = app.sessionStore,
                        onAutenticado = { n ->
                            nombre = n
                            autenticado = true
                        }
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Color.White),
                        Alignment.Center
                    ) {
                        Text(
                            "Conectado como $nombre\n\nEl chat llegará en el Bloque 2",
                            color = Texto,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
