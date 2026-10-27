package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tuapp.ui.theme.Azul
import com.tuapp.ui.theme.TextoSuave
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToReplyContainer(
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val umbralPx = with(density) { 72.dp.toPx() }
    val maxPx = with(density) { 96.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var activado by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        // Icono de fondo
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                .alpha((offsetX / umbralPx).coerceIn(0f, 1f))
        ) {
            Box(Modifier.size(32.dp).background(Azul, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offsetX = 0f; activado = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxPx)
                            val ahora = offsetX >= umbralPx
                            if (ahora != activado) {
                                activado = ahora
                                if (ahora) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = {
                            if (activado) onReply()
                            activado = false
                            scope.launch {
                                androidx.compose.animation.core.animate(offsetX, 0f, androidx.compose.animation.core.spring(0.6f, 400f)) { v, _ ->
                                    offsetX = v
                                }
                            }
                        },
                        onDragCancel = {
                            activado = false
                            scope.launch {
                                androidx.compose.animation.core.animate(offsetX, 0f, androidx.compose.animation.core.spring(0.6f, 400f)) { v, _ ->
                                    offsetX = v
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
