package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.ui.components.ImagenMensaje
import com.tuapp.ui.theme.*

@Composable
fun MessageBubble(
    m: Msg,
    propio: Boolean,
    onReply: () -> Unit,
    onVerImagen: (String) -> Unit
) {
    if (m.subidaEstado != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.padding(4.dp)) { ThumbnailSubiendo(m) }
        }
        return
    }

    val bg = if (propio) Color.White else Azul
    val fg = if (propio) Texto else Color.White
    val borderMod = if (propio) Modifier.border(1.dp, Borde, RoundedCornerShape(16.dp)) else Modifier

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(16.dp))
                .then(borderMod)
                .clickable { onReply() }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            m.cita?.let { c ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (propio) Color(0x0D000000) else Color(0x26FFFFFF),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Column {
                        Text(c.autor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = fg.copy(alpha = 0.9f))
                        Text(c.texto, fontSize = 12.5.sp, color = fg.copy(alpha = 0.9f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (m.texto.isNotEmpty()) {
                Text(m.texto, fontSize = 15.sp, color = fg, lineHeight = 20.sp)
            }

            if (m.imagenUrl.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ImagenMensaje(m.imagenUrl, m.imagenTamano, m.imagenDim, onVerImagen)
            }

            Row(Modifier.align(Alignment.End).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(m.hora, color = fg.copy(alpha = 0.7f), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                if (propio) {
                    Spacer(Modifier.width(4.dp))
                    CheckIcon(m.estado)
                }
            }
        }
    }
}

@Composable
fun CheckIcon(estado: String) {
    val (icon, tint) = when (estado) {
        "visto" -> Icons.Default.DoneAll to Azul
        "entregado" -> Icons.Default.DoneAll to Color(0xFF64748B)
        else -> Icons.Default.Done to Color(0xFF94A3B8)
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
}
