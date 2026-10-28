package com.tuapp.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
    
    val offsetX = remember { Animatable(0f) }
    var activado by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        // Icono de fondo
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                .alpha((offsetX.value / umbralPx).coerceIn(0f, 1f))
        ) {
            Box(Modifier.size(32.dp).background(Azul, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
            onDragStart = { offsetX = 0f; activado = false },
                        onDragStart = { 
                            activado = false 
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val nuevo = (offsetX.value + dragAmount).coerceIn(0f, maxPx)
                                offsetX.snapTo(nuevo)
                                val ahora = nuevo >= umbralPx
                                if (ahora != activado) {
                                    activado = ahora
                                    if (ahora) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        onDragEnd = {
                            if (activado) onReply()
                            activado = false
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            activado = false
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
