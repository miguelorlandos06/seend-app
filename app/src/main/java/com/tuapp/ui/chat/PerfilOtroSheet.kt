package com.tuapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onCerrar() }
    ) {
        Surface(
            Modifier.fillMaxHeight().width(360.dp).align(Alignment.CenterEnd)
                .clickable(enabled = false) {},
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = TextoSuave)
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(60.dp, 24.dp, 24.dp, 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        Modifier.size(100.dp).clip(CircleShape).background(AzulClaro),
                        contentAlignment = Alignment.Center
                    ) {
                        if (perfil.avatar.isNotBlank()) {
                            AsyncImage(
                                model = perfil.avatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                (perfil.nombre.firstOrNull() ?: '?').uppercase(),
                                color = AzulOscuro,
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(perfil.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    val (estadoTxt, estadoColor) = estadoPerfil(perfil)
                    Text(estadoTxt, fontSize = 14.sp, color = estadoColor, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(28.dp))

                    // Biografía
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biografía", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoSuave)
                        Text(
                            perfil.bio.ifBlank { "—" },
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                    Divider(color = Borde, thickness = 1.dp)
                }
            }
        }
    }
}

private fun estadoPerfil(p: PerfilPublico): Pair<String, Color> {
    if (p.enLinea) return "En línea" to Azul
    if (p.ultima == 0.0) return "Desconectado" to TextoSuave
    val diff = System.currentTimeMillis() / 1000.0 - p.ultima
    return when {
        diff < 60 -> "Últ. conexión hace un momento" to TextoSuave
        diff < 3600 -> "Últ. conexión hace ${(diff / 60).toInt()} min" to TextoSuave
        else -> "Desconectado" to TextoSuave
    }
}
