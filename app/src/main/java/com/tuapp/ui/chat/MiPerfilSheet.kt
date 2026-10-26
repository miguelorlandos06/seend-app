package com.tuapp.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuapp.ui.theme.*

@Composable
fun MiPerfilSheet(
    s: UiState,
    onCerrar: () -> Unit,
    onGuardar: (String, String) -> Unit,
    onCambiarAvatar: (Uri) -> Unit
) {
    var nombre by remember(s.nombre) { mutableStateOf(s.nombre) }
    var bio by remember(s.bioPropio) { mutableStateOf(s.bioPropio) }
    var avatarPreview by remember(s.avatarPropio) { mutableStateOf<String?>(s.avatarPropio.ifBlank { null }) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            avatarPreview = it.toString()
            onCambiarAvatar(it)
        }
    }

    // Fondo oscuro detrás
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onCerrar() }) {
        // Superficie del perfil (sin onClick, con Modifier)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Azul)
                    }
                    Text("Mi perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Borde, thickness = 1.dp)

                // Contenido
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar con cámara
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier.size(110.dp).clip(CircleShape).background(AzulClaro),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarPreview != null) {
                                AsyncImage(
                                    model = avatarPreview,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    (nombre.firstOrNull() ?: '?').uppercase(),
                                    color = AzulOscuro,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 42.sp
                                )
                            }
                        }
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Azul)
                                .clickable { picker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = Color.White,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Toca la cámara para cambiar tu foto", fontSize = 13.sp, color = TextoSuave)
                    Spacer(Modifier.height(28.dp))

                    // Campo nombre
                    Column(Modifier.fillMaxWidth()) {
                        Text("Nombre", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextoSuave)
                        Spacer(Modifier.height(8.dp))
                        var focusN by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = nombre,
                            onValueChange = { if (it.length <= 20) nombre = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Texto, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth()
                                .background(if (focusN) Color.White else Fondo, RoundedCornerShape(12.dp))
                                .border(1.5.dp, if (focusN) Azul else Borde, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .onFocusChangedModifier(focusN) { focusN = it }
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    // Campo bio
                    Column(Modifier.fillMaxWidth()) {
                        Text("Biografía", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextoSuave)
                        Spacer(Modifier.height(8.dp))
                        var focusB by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 120) bio = it },
                            maxLines = 4,
                            textStyle = LocalTextStyle.current.copy(color = Texto, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                .background(if (focusB) Color.White else Fondo, RoundedCornerShape(12.dp))
                                .border(1.5.dp, if (focusB) Azul else Borde, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .onFocusChangedModifier(focusB) { focusB = it }
                        )
                    }
                    Spacer(Modifier.height(28.dp))

                    // Botón guardar
                    Button(
                        onClick = { onGuardar(nombre.trim(), bio.trim()) },
                        enabled = nombre.isNotBlank() && !s.guardandoPerfil,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Azul)
                    ) {
                        if (s.guardandoPerfil) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar cambios", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Cerrar sesión
                    Button(
                        onClick = onCerrar,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEF2F2),
                            contentColor = Color(0xFFB91C1C)
                        )
                    ) {
                        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.onFocusChangedModifier(initial: Boolean, onChange: (Boolean) -> Unit): Modifier {
    return this.then(
        androidx.compose.ui.focus.onFocusChanged { onChange(it.isFocused) }
    )
}
