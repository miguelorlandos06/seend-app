package com.tuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuapp.ui.theme.AzulClaro
import com.tuapp.ui.theme.AzulOscuro

@Composable
fun Avatar(avatarUrl: String, nombre: String, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(AzulClaro),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = (nombre.firstOrNull() ?: '?').uppercase(),
                color = AzulOscuro,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.6f).sp
            )
        }
    }
}
