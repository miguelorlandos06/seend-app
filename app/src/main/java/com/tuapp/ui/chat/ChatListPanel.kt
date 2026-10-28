package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.ui.components.Avatar
import com.tuapp.ui.theme.*

@Composable
fun ChatListPanel(
    s: UiState,
    onAbrirChat: (String) -> Unit,
    onMostrarUsuarios: () -> Unit,
    onVolverHome: () -> Unit,
    onAbrirMiPerfil: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (s.mostrandoUsuarios) {
                    IconButton(onClick = onVolverHome) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Azul) }
                }
                
                if (s.mostrandoUsuarios) {
                    Text("Usuarios", modifier = Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                } else {
                    TituloReactivo(estado = s.estadoConexion, modifier = Modifier.weight(1f))
                }

                if (!s.mostrandoUsuarios) {
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).clickable { onAbrirMiPerfil() }.padding(4.dp, 4.dp, 8.dp, 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar("", s.nombre, 36.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(s.nombre, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 90.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            if (!s.mostrandoUsuarios) {
                FloatingActionButton(onClick = onMostrarUsuarios, containerColor = Azul, contentColor = Color.White, shape = CircleShape) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo chat")
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (s.mostrandoUsuarios) {
                ListaUsuarios(s, onAbrirChat)
            } else {
                ListaChats(s, onAbrirChat)
            }
        }
    }
}

@Composable
private fun ListaChats(s: UiState, onAbrirChat: (String) -> Unit) {
    if (s.chats.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No tienes conversaciones.\nPulsa el botón + para empezar.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(s.chats, key = { it.otro }) { c ->
            ChatRow(c = c, escribiendo = s.escribiendo[c.otro] == true, activo = s.chatActual == c.otro, yo = s.nombre, onClick = { onAbrirChat(c.otro) })
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }
    }
}

@Composable
private fun ChatRow(c: ChatItem, escribiendo: Boolean, activo: Boolean, yo: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (activo) AzulClaro else Color.Transparent).clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(c.avatar, c.otro, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.otro, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.ultimo.hora, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = when {
                    escribiendo -> "Escribiendo..."
                    c.ultimo.imagen -> "Imagen"
                    else -> (if (c.ultimo.de == yo) "Tú: " else "") + c.ultimo.texto
                }
                Text(
                    preview, Modifier.weight(1f), fontSize = 14.5.sp, color = Color.Black,
                    fontStyle = if (escribiendo) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = if (escribiendo) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (c.ultimo.de == yo && !escribiendo) { Spacer(Modifier.width(4.dp)); CheckIcon(c.ultimo.estado) }
                if (c.noLeidos > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.background(Azul, CircleShape).padding(horizontal = 7.dp, vertical = 2.dp)) {
                        Text("${c.noLeidos}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckIcon(estado: String) {
    val (icon, tint) = when (estado) {
        "visto" -> Icons.Default.DoneAll to Azul
        "entregado" -> Icons.Default.DoneAll to Color(0xFF64748B)
        else -> Icons.Default.Done to Color(0xFF94A3B8)
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
}

@Composable
private fun ListaUsuarios(s: UiState, onAbrirChat: (String) -> Unit) {
    val otros = s.usuarios.filter { it.nombre != s.nombre }
    if (otros.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No hay otros usuarios registrados todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
        return
    }
    val enLinea = otros.filter { it.enLinea }
    val desconectados = otros.filter { !it.enLinea }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (enLinea.isNotEmpty()) {
            item { Separador("En línea") }
            items(enLinea, key = { it.nombre }) { u -> UsuarioRow(u, onAbrirChat) }
        }
        if (desconectados.isNotEmpty()) {
            item { Separador("Desconectados") }
            items(desconectados, key = { it.nombre }) { u -> UsuarioRow(u, onAbrirChat) }
        }
    }
}

@Composable
private fun Separador(texto: String) {
    Text(texto.uppercase(), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp, vertical = 14.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 0.8.sp)
}

@Composable
private fun UsuarioRow(u: Usuario, onAbrirChat: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onAbrirChat(u.nombre) }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(u.avatar, u.nombre, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            // En vez de "En línea" o "Últ. conexión", mostramos la bio o un texto por defecto
            val infoTexto = if (u.bio.isNotBlank()) u.bio else "¡Hola! Estoy usando Seend"
            Text(infoTexto, fontSize = 14.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
}
