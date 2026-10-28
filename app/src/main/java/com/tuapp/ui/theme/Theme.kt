package com.tuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Azul        = Color(0xFF2563EB)
val AzulOscuro  = Color(0xFF1E40AF)
val AzulClaro   = Color(0xFFDBEAFE)
val Fondo       = Color(0xFFF8FAFC)
val Borde       = Color(0xFFE2E8F0)
val Texto       = Color(0xFF0F172A)
val TextoSuave  = Color(0xFF64748B)

val AzulDark       = Color(0xFF60A5FA)
val FondoDark      = Color(0xFF0F172A)
val BordeDark      = Color(0xFF334155)
val TextoDark      = Color(0xFFF1F5F9)
val TextoSuaveDark = Color(0xFF94A3B8)

private val LightScheme = lightColorScheme(
    primary = Azul, onPrimary = Color.White,
    background = Fondo, onBackground = Texto,
    surface = Color.White, onSurface = Texto,
    surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = TextoSuave,
    outline = Borde, error = Color(0xFFB91C1C)
)
private val DarkScheme = darkColorScheme(
    primary = AzulDark, onPrimary = FondoDark,
    background = FondoDark, onBackground = TextoDark,
    surface = Color(0xFF1E293B), onSurface = TextoDark,
    surfaceVariant = Color(0xFF334155), onSurfaceVariant = TextoSuaveDark,
    outline = BordeDark, error = Color(0xFFF87171)
)

@Composable
fun AppTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) DarkScheme else LightScheme,
        content = content
    )
}

val ShapeCard  = RoundedCornerShape(16.dp)
val ShapeTab   = RoundedCornerShape(10.dp)
val ShapeInput = RoundedCornerShape(9.dp)
val ShapeMsg   = RoundedCornerShape(12.dp)
val ShapePill  = RoundedCornerShape(22.dp)
