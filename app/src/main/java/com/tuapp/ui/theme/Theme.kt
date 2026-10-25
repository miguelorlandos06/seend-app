package com.tuapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Azul       = Color(0xFF2563EB)
val AzulOscuro = Color(0xFF1E40AF)
val AzulClaro  = Color(0xFFDBEAFE)
val Fondo      = Color(0xFFF8FAFC)
val Borde      = Color(0xFFE2E8F0)
val Texto      = Color(0xFF0F172A)
val TextoSuave = Color(0xFF64748B)

private val scheme = lightColorScheme(
    primary = Azul, onPrimary = Color.White,
    background = Fondo, onBackground = Texto,
    surface = Color.White, onSurface = Texto,
    outline = Borde, error = Color(0xFFB91C1C)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

val ShapeCard  = RoundedCornerShape(16.dp)
val ShapeTab   = RoundedCornerShape(10.dp)
val ShapeInput = RoundedCornerShape(9.dp)
val ShapeMsg   = RoundedCornerShape(12.dp)
val ShapePill  = RoundedCornerShape(22.dp)
