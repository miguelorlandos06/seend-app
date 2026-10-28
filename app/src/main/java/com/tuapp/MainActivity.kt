package com.tuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.ui.auth.AuthScreen
import com.tuapp.ui.chat.ChatScreen
import com.tuapp.ui.chat.ChatViewModel
import com.tuapp.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            var nombreInicial by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                nombreInicial = app.sessionStore.nombre() ?: ""
            }

            val oscuroPref by app.temaPrefs.oscuro.collectAsState(initial = isSystemInDarkTheme())
            val scope = rememberCoroutineScope()

            val vm: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(app.ws, app.sessionStore, app.repo, app.uploader, app.monitor, app.cola, nombreInicial) as T
                }
            })

            val s by vm.ui.collectAsState()

            AppTheme(oscuro = oscuroPref) {
                if (!s.autenticado && nombreInicial.isBlank()) {
                    AuthScreen(app.ws, app.sessionStore, onAutenticado = { vm.marcarAutenticado(it) })
                } else {
                    ChatScreen(
                        vm = vm,
                        onToggleTema = { oscuro -> scope.launch { app.temaPrefs.set(oscuro) } },
                        isOscuro = oscuroPref
                    )
                }
            }
        }
    }
}
