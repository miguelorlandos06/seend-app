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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    onCambiarAvatar: (Uri) -> Unit,
    onToggleTema: (Boolean) -> Unit,
    isOscuro: Boolean
) {
    var editando by remember { mutableStateOf(false) }
    var nombre by remember(s.nombre) { mutableStateOf(s.nombre) }
    var bio by remember(s.bioPropio) { mutableStateOf(s.bioPropio) }
    var avatarPreview by remember(s.avatarPropio) { mutableStateOf<String?>(s.avatarPropio.ifBlank { null }) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            avatarPreview = it.toString()
            onCambiarAvatar(it)
        }
    }

    LaunchedEffect(s.mostrarMiPerfil) {
        if (s.mostrarMiPerfil) {
            editando = false
            nombre = s.nombre
            bio = s.bioPropio
            avatarPreview = s.avatarPropio.ifBlank { null }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onCerrar() }) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (editando) {
                            editando = false
                            nombre = s.nombre
                            bio = s.bioPropio
                            avatarPreview = s.avatarPropio.ifBlank { null }
                        } else {
                            onCerrar()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Azul)
                    }

                    Text(
                        if (editando) "Editar perfil" else "Mi perfil",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    if (!editando) {
                        TextButton(onClick = { editando = true }) {
                            Icon(Icons.Default.Edit, null, tint = Azul, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Editar perfil", color = Azul, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TextButton(
                            onClick = { onGuardar(nombre.trim(), bio.trim()) },
                            enabled = nombre.isNotBlank() && !s.guardandoPerfil
                        ) {
                            if (s.guardandoPerfil) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Azul, strokeWidth = 2.dp)
                            } else {
                                Text("Guardar", color = Azul, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier.size(110.dp).clip(CircleShape).background(AzulClaro),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarPreview != null) {
                                AsyncImage(model = avatarPreview, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text((nombre.firstOrNull() ?: '?').uppercase(), color = AzulOscuro, fontWeight = FontWeight.Bold, fontSize = 42.sp)
                            }
                        }
                        if (editando) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Azul).clickable { picker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (editando) {
                        Text("Toca la cámara para cambiar tu foto", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(28.dp))

                        Column(Modifier.fillMaxWidth()) {
                            Text("Nombre", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            var focusN by remember { mutableStateOf(false) }
                            BasicTextField(
                                value = nombre,
                                onValueChange = { if (it.length <= 20) nombre = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                                modifier = Modifier.fillMaxWidth()
                                    .background(if (focusN) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (focusN) Azul else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .onFocusChanged { focusN = it.isFocused }
                            )
                        }
                        Spacer(Modifier.height(20.dp))

                        Column(Modifier.fillMaxWidth()) {
                            Text("Biografía", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            var focusB by remember { mutableStateOf(false) }
                            BasicTextField(
                                value = bio,
                                onValueChange = { if (it.length <= 120) bio = it },
                                maxLines = 4,
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                    .background(if (focusB) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (focusB) Azul else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .onFocusChanged { focusB = it.isFocused }
                            )
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(s.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        if (s.bioPropio.isNotBlank()) {
                            Text(s.bioPropio, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                        } else {
                            Text("Sin biografía", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(32.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    if (!editando) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tema oscuro", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = isOscuro,
                                onCheckedChange = { onToggleTema(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Azul)
                            )
                        }

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
}
