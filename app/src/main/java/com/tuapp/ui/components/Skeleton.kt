package com.tuapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuapp.ui.theme.Borde

@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9), Color(0xFFE2E8F0)),
        start = Offset(x, 0f), end = Offset(x + 400f, 400f)
    )
}

@Composable
fun SkeletonBox(mod: Modifier, radius: Dp = 6.dp) {
    Box(mod.clip(RoundedCornerShape(radius)).background(ShimmerBrush()))
}

@Composable
fun SkeletonCircle(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(ShimmerBrush()))
}

@Composable
fun SkeletonChats() {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        repeat(8) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.height(14.dp).weight(1f), 4.dp)
                        Spacer(Modifier.width(8.dp))
                        SkeletonBox(Modifier.width(30.dp).height(10.dp), 4.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.7f).height(12.dp), 4.dp)
                }
            }
            Divider(color = Borde, thickness = 1.dp)
        }
    }
}

@Composable
fun SkeletonConversacion() {
    val patrones = listOf(0.55f to true, 0.70f to false, 0.40f to true, 0.65f to false, 0.50f to true, 0.75f to false)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        patrones.forEach { (ancho, otro) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (otro) Arrangement.Start else Arrangement.End) {
                SkeletonBox(Modifier.fillMaxWidth(ancho).height(if (ancho > 0.6f) 56.dp else 38.dp), 12.dp)
            }
        }
    }
}
