package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.ui.theme.Azul
import com.tuapp.ui.theme.Texto

fun fmtBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.1f KB".format(b / 1024.0)
    else -> "%.1f MB".format(b / (1024.0 * 1024.0))
}

@Composable
fun ThumbnailSubiendo(m: Msg) {
    val progreso = m.progreso.coerceIn(0f, 1f)
    Box(
        Modifier
            .size(220.dp, 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
    ) {
        if (m.dimensiones != null) {
            Text(
                m.dimensiones!!,
                Modifier.align(Alignment.Center).offset(y = (-22).dp),
                fontSize = 12.sp,
                color = Texto.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                when (m.subidaEstado) {
                    SubidaEstado.Pendiente, SubidaEstado.Subiendo -> {
                        CircularProgressIndicator(
                            progress = { progreso },
                            modifier = Modifier.fillMaxSize(),
                            color = Azul,
                            strokeWidth = 3.dp,
                            trackColor = Color(0x33000000)
                        )
                        Icon(Icons.Default.Close, null, tint = Azul, modifier = Modifier.size(22.dp))
                    }
                    SubidaEstado.Fallida -> Icon(
                        Icons.Default.Image, null, tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(26.dp)
                    )
                    else -> Icon(Icons.Default.Image, null, tint = Azul, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            val txt = when (m.subidaEstado) {
                SubidaEstado.Subiendo ->
                    "${fmtBytes(m.bytesSubidos)} / ${fmtBytes(m.bytesTotales)}"
                SubidaEstado.Pendiente -> fmtBytes(m.bytesTotales)
                SubidaEstado.Fallida -> "Error al subir"
                else -> m.imagenTamano
            }
            Text(txt, fontSize = 11.sp, color = Texto.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
        }
        Text(
            if (m.subidaEstado == SubidaEstado.Subiendo)
                fmtBytes((m.bytesTotales - m.bytesSubidos).coerceAtLeast(0))
            else m.imagenTamano,
            Modifier.align(Alignment.BottomEnd).padding(8.dp),
            fontSize = 10.5.sp, color = Color(0xFF0F172A).copy(alpha = 0.65f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
