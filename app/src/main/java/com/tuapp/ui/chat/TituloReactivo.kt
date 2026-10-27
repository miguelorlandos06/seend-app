package com.tuapp.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tuapp.data.EstadoConexion
import com.tuapp.ui.theme.Azul
import com.tuapp.ui.theme.Texto

@Composable
fun TituloReactivo(estado: EstadoConexion, modifier: Modifier = Modifier) {
    val (texto, color) = when (estado) {
        is EstadoConexion.Conectando -> "Conectando..." to Color(0xFFB45309)
        is EstadoConexion.Error -> "Esperando red..." to Color(0xFFB45309)
        is EstadoConexion.Desconectado -> "Reconectando..." to Color(0xFFB45309)
        is EstadoConexion.Conectado -> "Seend" to Texto
    }
    AnimatedContent(
        targetState = texto,
        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
        label = "titulo", modifier = modifier
    ) { t ->
        Text(t, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
