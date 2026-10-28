package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuapp.ui.theme.*

@Composable
fun PerfilOtroSheet(
    perfil: PerfilPublico,
    onCerrar: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCerrar) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("Perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            }
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Avatar
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(130.dp).clip(CircleShape).background(AzulClaro),
                    contentAlignment = Alignment.Center
                ) {
                    if (perfil.avatar.isNotBlank()) {
                        AsyncImage(model = perfil.avatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text((perfil.nombre.firstOrNull() ?: '?').uppercase(), color = AzulOscuro, fontWeight = FontWeight.Bold, fontSize = 50.sp)
                    }
                }
            }

            // Info
            FilaPerfilOtro(Icons.Default.AlternateEmail, "Nombre de usuario", "@${perfil.nombre}")
            FilaPerfilOtro(Icons.Default.Info, "Info.", perfil.bio.ifBlank { "Sin biografía" })
            
            Spacer(Modifier.height(16.dp))
            
            val (estado, _) = if (perfil.enLinea) "En línea" to Azul else "Últ. conexión: ${formatearHora(perfil.ultima)}" to TextoSuave
            Text(estado, Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilaPerfilOtro(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(valor, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatearHora(ts: Double): String {
    if (ts == 0.0) return "Desconocido"
    val diff = System.currentTimeMillis() / 1000.0 - ts
    return when {
        diff < 60 -> "hace un momento"
        diff < 3600 -> "hace ${(diff / 60).toInt()} min"
        else -> "hace mucho tiempo"
    }
}
