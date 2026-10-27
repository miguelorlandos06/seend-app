package com.tuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.ui.auth.AuthScreen
import com.tuapp.ui.chat.ChatScreen
import com.tuapp.ui.chat.ChatViewModel
import com.tuapp.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            var autenticado by rememberSaveable { mutableStateOf(false) }
            val oscuroPref by app.temaPrefs.oscuro.collectAsState(initial = isSystemInDarkTheme())

            LaunchedEffect(Unit) {
                if (app.sessionStore.token() != null) autenticado = true
            }

            AppTheme(oscuro = oscuroPref) {
                if (!autenticado) {
                    AuthScreen(
                        ws = app.ws,
                        store = app.sessionStore,
                        onAutenticado = { autenticado = true }
                    )
                } else {
                    val vm: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ChatViewModel(app.ws, app.sessionStore, app.repo, app.uploader) as T
                        }
                    })
                    ChatScreen(
                        vm = vm,
                        onToggleTema = { oscuro -> app.temaPrefs.set(oscuro) },
                        isOscuro = oscuroPref
                    )
                }
            }
        }
    }
}
