package com.tuapp.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuapp.ui.theme.Fondo

@Composable
fun ChatScreen(vm: ChatViewModel) {
    LaunchedEffect(Unit) { vm.iniciar() }
    val s by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var imagenVisor by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = s.chatActual != null) { vm.cerrarChat() }
    BackHandler(enabled = s.chatActual == null && s.mostrandoUsuarios) { vm.volverAHome() }
    BackHandler(enabled = imagenVisor != null) { imagenVisor = null }

    Box(Modifier.fillMaxSize().background(Fondo)) {
        if (s.chatActual == null) {
            ChatListPanel(
                s = s,
                onAbrirChat = vm::abrirChat,
                onMostrarUsuarios = vm::mostrarUsuarios,
                onVolverHome = vm::volverAHome,
                onAbrirMiPerfil = vm::abrirMiPerfil
            )
        } else {
            ConversationPanel(
                s = s,
                onCerrar = { vm.cerrarChat() },
                onEnviar = { texto -> vm.enviarTexto(texto) },
                onEnviarImagen = { uri, texto -> vm.enviarImagen(ctx, uri, texto) },
                onResponderA = { m -> vm.responderA(m) },
                onTextoCambia = { t -> vm.onTextoCambia(t) },
                onVerImagen = { url -> imagenVisor = url },
                onPerfilOtro = { vm.pedirPerfil(s.chatActual!!) }
            )
        }
    }

    if (s.mostrarMiPerfil) {
        MiPerfilSheet(
            s = s,
            onCerrar = { vm.cerrarMiPerfil() },
            onGuardar = { nombre, bio -> vm.guardarPerfil(nombre, bio) },
            onCambiarAvatar = { uri -> vm.cambiarAvatar(ctx, uri) }
        )
    }

    s.perfilOtro?.let { p ->
        PerfilOtroSheet(perfil = p, onCerrar = { vm.cerrarPerfilOtro() })
    }

    imagenVisor?.let { url ->
        Dialog(
            onDismissRequest = { imagenVisor = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier.fillMaxSize().background(Color(0xE60F172A)).clickable { imagenVisor = null },
                Alignment.Center
            ) {
                AsyncImage(
                    model = url, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.92f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
