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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // Estados para las sub-pantallas de edición
    var editandoCampo by remember { mutableStateOf<String?>(null) } // "nombre" o "bio"
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
            nombre = s.nombre
            bio = s.bioPropio
            avatarPreview = s.avatarPropio.ifBlank { null }
            editandoCampo = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onCerrar() }) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (editandoCampo != null) {
                // ---- SUB-PANTALLA DE EDICIÓN ----
                PantallaEdicion(
                    titulo = if (editandoCampo == "nombre") "Nombre" else "Info.",
                    valor = if (editandoCampo == "nombre") nombre else bio,
                    limite = if (editandoCampo == "nombre") 20 else 120,
                    onValorChange = { 
                        if (editandoCampo == "nombre") nombre = it else bio = it 
                    },
                    onCerrar = { 
                        if (editandoCampo == "nombre") nombre = s.nombre else bio = s.bioPropio
                        editandoCampo = null 
                    },
                    onGuardar = {
                        onGuardar(nombre, bio)
                        editandoCampo = null
                    }
                )
            } else {
                // ---- PANTALLA PRINCIPAL DE PERFIL ----
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Top bar
                    Row(
                        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCerrar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Azul)
                        }
                        Text("Perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                    // Avatar
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                Modifier.size(130.dp).clip(CircleShape).background(AzulClaro),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarPreview != null) {
                                    AsyncImage(model = avatarPreview, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text((nombre.firstOrNull() ?: '?').uppercase(), color = AzulOscuro, fontWeight = FontWeight.Bold, fontSize = 50.sp)
                                }
                            }
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(Azul).clickable { picker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Opciones
                    FilaPerfil(Icons.Default.Person, "Nombre", s.nombre) { editandoCampo = "nombre" }
                    FilaPerfil(Icons.Default.Info, "Info.", s.bioPropio.ifBlank { "Completar sección Info." }) { editandoCampo = "bio" }
                    FilaPerfil(Icons.Default.AlternateEmail, "Nombre de usuario", "@${s.nombre}") { /* No se puede editar el username aquí por ahora */ }

                    Spacer(Modifier.height(32.dp))

                    // Tema Oscuro
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
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

                    // Cerrar Sesión
                    Button(
                        onClick = onCerrar, // O el logout real
                        modifier = Modifier.fillMaxWidth().padding(24.dp).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFB91C1C))
                    ) {
                        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPerfil(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
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

@Composable
private fun PantallaEdicion(
    titulo: String,
    valor: String,
    limite: Int,
    onValorChange: (String) -> Unit,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit
) {
    var texto by remember { mutableStateOf(valor) }
    
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCerrar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = texto,
            onValueChange = { if (it.length <= limite) texto = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Azul,
                focusedLabelColor = Azul,
                cursorColor = Azul,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            label = { Text(if (titulo == "Nombre") "Tu nombre" else "¿Qué sucede?") },
            supportingText = { Text("${texto.length}/$limite", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) }
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = { onGuardar() },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Azul)
        ) {
            Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
