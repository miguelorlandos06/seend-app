package com.tuapp.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.data.EstadoConexion
import com.tuapp.ui.components.Avatar
import com.tuapp.ui.theme.*

@Composable
fun ConversationPanel(
    s: UiState,
    onCerrar: () -> Unit,
    onEnviar: (String) -> Unit,
    onEnviarImagen: (Uri, String) -> Unit,
    onResponderA: (Msg?) -> Unit,
    onTextoCambia: (String) -> Unit,
    onVerImagen: (String) -> Unit,
    onPerfilOtro: () -> Unit
) {
    val otro = s.chatActual ?: return
    val listaState = rememberLazyListState()
    var texto by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onEnviarImagen(it, texto) }
    }

    val mensajesVisibles = remember(s.mensajes, s.subiendo, otro) {
        (s.mensajes + s.subiendo.filter { it.para == otro }).sortedBy { it.ts }
    }

    LaunchedEffect(mensajesVisibles.size) {
        if (mensajesVisibles.isNotEmpty()) {
            listaState.animateScrollToItem(mensajesVisibles.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        // ---- Header ----
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCerrar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Azul)
            }
            val chat = s.chats.find { it.otro == otro }
            val usuario = s.usuarios.find { it.nombre == otro }
            val avatarUrl = chat?.avatar ?: usuario?.avatar ?: ""
            Row(
                Modifier.weight(1f).clickable { onPerfilOtro() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(avatarUrl, otro, 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(otro, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    EstadoHeaderReactivo(s = s, otro = otro)
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // ---- Mensajes ----
        if (mensajesVisibles.isEmpty() && s.estadoConexion is EstadoConexion.Conectando) {
            com.tuapp.ui.components.SkeletonConversacion()
        } else {
            LazyColumn(
                state = listaState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(mensajesVisibles, key = { it.id }) { m ->
                    MessageBubble(
                        m = m,
                        propio = m.de == s.nombre,
                        onReply = { onResponderA(m) },
                        onVerImagen = onVerImagen
                    )
                }
            }
        }

        // ---- Barra de respuesta ----
        s.respondiendoA?.let { r ->
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(36.dp).background(Azul, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (r.de == s.nombre) "Tú" else r.de, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Azul)
                    Text(r.texto.ifEmpty { "[imagen]" }, fontSize = 13.sp, color = TextoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onResponderA(null) }) {
                    Icon(Icons.Default.Close, null, tint = TextoSuave, modifier = Modifier.size(20.dp))
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }

        // ---- Input ----
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { picker.launch("image/*") }) {
                Icon(Icons.Default.AttachFile, null, tint = TextoSuave)
            }
            BasicTextField(
                value = texto,
                onValueChange = { texto = it; onTextoCambia(it) },
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp, max = 120.dp)
                    .background(if (focus) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                    .border(1.5.dp, if (focus) Azul else MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .onFocusChanged { focus = it.isFocused }
            )
            Spacer(Modifier.width(8.dp))
            val activo = texto.isNotBlank()
            FilledIconButton(
                onClick = { onEnviar(texto); texto = "" },
                enabled = activo,
                modifier = Modifier.size(46.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Azul, contentColor = Color.White,
                    disabledContainerColor = Color(0xFFCBD5E1), disabledContentColor = Color.White
                )
            ) { Icon(Icons.AutoMirrored.Filled.Send, null) }
        }
    }
}

@Composable
private fun EstadoHeaderReactivo(s: UiState, otro: String) {
    val (texto, esAnimado) = when (s.estadoConexion) {
        is EstadoConexion.Conectando -> "Reconectando..." to true
        is EstadoConexion.Error -> "Sin conexión" to true
        is EstadoConexion.Desconectado -> "Sin conexión" to true
        else -> {
            if (s.escribiendo[otro] == true) "Escribiendo..." to false
            else {
                val chat = s.chats.find { it.otro == otro }
                val usuario = s.usuarios.find { it.nombre == otro }
                val enLinea = chat?.enLinea ?: usuario?.enLinea ?: false
                val ultima = chat?.ultima ?: usuario?.ultima ?: 0.0
                if (enLinea) "En línea" to false
                else if (ultima == 0.0) "Desconectado" to false
                else {
                    val diff = System.currentTimeMillis() / 1000.0 - ultima
                    when {
                        diff < 60 -> "Últ. conexión hace un momento" to false
                        diff < 3600 -> "Últ. conexión hace ${(diff / 60).toInt()} min" to false
                        else -> "Desconectado" to false
                    }
                }
            }
        }
    }

    val brush = if (esAnimado) {
        val transition = rememberInfiniteTransition(label = "shimmerHeader")
        val x by transition.animateFloat(
            initialValue = -200f, targetValue = 400f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "xHeader"
        )
        Brush.linearGradient(
            colors = listOf(Color.Black, Color(0xFF64748B), Color.Black),
            start = Offset(x, 0f), end = Offset(x + 200f, 200f)
        )
    } else {
        Brush.linearGradient(listOf(Color.Black, Color.Black))
    }

    Text(texto, style = TextStyle(brush = brush, fontSize = 12.5.sp, fontWeight = FontWeight.Medium))
}
