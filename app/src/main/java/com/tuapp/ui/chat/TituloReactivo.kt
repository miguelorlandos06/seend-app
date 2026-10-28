package com.tuapp.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tuapp.data.EstadoConexion

@Composable
fun TituloReactivo(estado: EstadoConexion, modifier: Modifier = Modifier) {
    val texto = when (estado) {
        is EstadoConexion.Conectando -> "Conectando..."
        is EstadoConexion.Error -> "Esperando red..."
        is EstadoConexion.Desconectado -> "Reconectando..."
        is EstadoConexion.Conectado -> "Seend"
    }

    val esAnimado = estado !is EstadoConexion.Conectado
    val brush = if (esAnimado) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val x by transition.animateFloat(
            initialValue = -500f, targetValue = 1000f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "x"
        )
        Brush.linearGradient(
            colors = listOf(Color.Black, Color(0xFF64748B), Color.Black), // Ola gris sobre negro
            start = Offset(x, 0f), end = Offset(x + 500f, 500f)
        )
    } else {
        Brush.linearGradient(listOf(Color.Black, Color.Black))
    }

    Box(modifier) {
        Text(
            text = texto,
            style = TextStyle(
                brush = brush,
                fontSize = 19.sp, // Un "tin" más grande que 18.sp
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}
