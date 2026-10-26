package com.tuapp.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.ui.theme.Fondo

@Composable
fun ChatScreen(vm: ChatViewModel) {
    LaunchedEffect(Unit) { vm.iniciar() }
    val s by vm.ui.collectAsStateWithLifecycle()

    BackHandler(enabled = s.chatActual != null) { vm.cerrarChat() }
    BackHandler(enabled = s.chatActual == null && s.mostrandoUsuarios) { vm.volverAHome() }

    Box(Modifier.fillMaxSize().background(Fondo)) {
        if (s.chatActual == null) {
            ChatListPanel(
                s = s,
                onAbrirChat = vm::abrirChat,
                onMostrarUsuarios = vm::mostrarUsuarios,
                onVolverHome = vm::volverAHome
            )
        } else {
            ConversationPanel(
                s = s,
                onCerrar = { vm.cerrarChat() },
                onEnviar = { texto -> vm.enviarTexto(texto) },
                onResponderA = { m -> vm.responderA(m) },
                onTextoCambia = { t -> vm.onTextoCambia(t) },
                onVerImagen = { url -> /* visor en Bloque 3 */ }
            )
        }
    }
}
