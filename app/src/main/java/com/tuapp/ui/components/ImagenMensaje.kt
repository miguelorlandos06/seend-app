package com.tuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuapp.ui.theme.Azul

@Composable
fun ImagenMensaje(url: String, tamano: String, dim: String, blurhash: String, onVer: (String) -> Unit) {
    var cargada by remember { mutableStateOf(false) }
    if (!cargada) {
        Box(Modifier.size(220.dp, 160.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0))) {
            if (blurhash.isNotBlank()) {
                BlurHashImage(blurhash = blurhash, modifier = Modifier.fillMaxSize().blur(16.dp))
            }
            Box(Modifier.fillMaxSize().background(Color(0x33000000)))
            IconButton(onClick = { cargada = true }, modifier = Modifier.align(Alignment.Center).size(48.dp).background(Color(0xCCFFFFFF), RoundedCornerShape(50))) {
                Icon(Icons.Default.Image, null, tint = Azul, modifier = Modifier.size(24.dp))
            }
            if (tamano.isNotEmpty() || dim.isNotEmpty()) {
                val texto = listOf(dim, tamano).filter { it.isNotEmpty() }.joinToString(" · ")
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(texto, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        Box(Modifier.widthIn(max = 250.dp).heightIn(max = 250.dp).clip(RoundedCornerShape(12.dp)).clickable { onVer(url) }) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}
