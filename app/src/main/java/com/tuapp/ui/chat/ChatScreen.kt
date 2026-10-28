package com.tuapp.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: ChatViewModel, onToggleTema: (Boolean) -> Unit, isOscuro: Boolean) {
    LaunchedEffect(Unit) { vm.iniciar() }
    val s by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var imagenVisor by remember { mutableStateOf<String?>(null) }
    
    // Fix: Snackbar para errores
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(s.error) {
        s.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.limpiarError()
        }
    }

    BackHandler(enabled = s.chatActual != null) { vm.cerrarChat() }
    BackHandler(enabled = s.chatActual == null && s.mostrandoUsuarios) { vm.volverAHome() }
    BackHandler(enabled = s.mostrarMiPerfil) { vm.cerrarMiPerfil() }
    BackHandler(enabled = s.perfilOtro != null) { vm.cerrarPerfilOtro() }
    BackHandler(enabled = imagenVisor != null) { imagenVisor = null }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (s.chatActual == null) {
            ChatListPanel(s, vm::abrirChat, vm::mostrarUsuarios, vm::volverAHome, vm::abrirMiPerfil)
        } else {
            ConversationPanel(s, vm::cerrarChat, vm::enviarTexto, { uri, texto -> vm.enviarImagen(ctx, uri, texto) }, vm::responderA, vm::onTextoCambia, { url -> imagenVisor = url }, { vm.pedirPerfil(s.chatActual!!) })
        }
        
        // Snackbar host
        Box(Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (s.mostrarMiPerfil) {
        MiPerfilSheet(
            s = s, onCerrar = { vm.cerrarMiPerfil() }, onGuardar = { nombre, bio -> vm.guardarPerfil(nombre, bio) },
            onCambiarAvatar = { uri -> vm.cambiarAvatar(ctx, uri) }, onToggleTema = onToggleTema, isOscuro = isOscuro,
            onCerrarSesion = { vm.cerrarSesion() }
        )
    }

    s.perfilOtro?.let { p -> PerfilOtroSheet(perfil = p, onCerrar = { vm.cerrarPerfilOtro() }) }

    imagenVisor?.let { url ->
        Dialog(onDismissRequest = { imagenVisor = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color(0xE60F172A)).clickable { imagenVisor = null }, Alignment.Center) {
                AsyncImage(model = url, contentDescription = "Visor", modifier = Modifier.fillMaxWidth(0.92f), contentScale = ContentScale.Fit)
            }
        }
    }
}
