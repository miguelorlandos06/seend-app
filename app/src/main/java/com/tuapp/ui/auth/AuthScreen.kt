package com.tuapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.R
import com.tuapp.data.ChatWebSocket
import com.tuapp.data.SessionStore
import com.tuapp.ui.theme.Azul
import com.tuapp.ui.theme.AzulClaro
import com.tuapp.ui.theme.Borde
import com.tuapp.ui.theme.Fondo
import com.tuapp.ui.theme.ShapeCard
import com.tuapp.ui.theme.ShapeInput
import com.tuapp.ui.theme.ShapeTab
import com.tuapp.ui.theme.Texto
import com.tuapp.ui.theme.TextoSuave

@Composable
fun AuthScreen(
    ws: ChatWebSocket,
    store: SessionStore,
    onAutenticado: (String) -> Unit
) {
    val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(ws, store) as T
        }
    })
    LaunchedEffect(Unit) { vm.iniciar() }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.autenticado) {
        if (ui.autenticado) onAutenticado(ui.nombre)
    }

    var modo by remember { mutableStateOf("login") }
    var nombre by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(Fondo).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            Modifier.widthIn(max = 400.dp).padding(20.dp),
            shape = ShapeCard,
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(AzulClaro)
                        .align(Alignment.CenterHorizontally),
                    Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_chat_logo),
                        contentDescription = null,
                        tint = Azul,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Mensajería", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (modo == "login") "Inicia sesión para continuar" else "Crea una cuenta nueva",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.5.sp,
                    color = TextoSuave
                )
                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth().background(Fondo, ShapeTab).padding(4.dp)) {
                    TabBtn("Iniciar sesión", modo == "login", Modifier.weight(1f)) {
                        modo = "login"; vm.limpiarError()
                    }
                    TabBtn("Crear cuenta", modo == "registro", Modifier.weight(1f)) {
                        modo = "registro"; vm.limpiarError()
                    }
                }
                Spacer(Modifier.height(22.dp))

                ui.error?.let { err ->
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(10.dp, 12.dp)
                    ) {
                        Text(err, color = Color(0xFFB91C1C), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Campo("Nombre de usuario", nombre, { nombre = it })
                Spacer(Modifier.height(14.dp))
                Campo("Contraseña", pass, { pass = it }, password = true)
                if (modo == "registro") {
                    Spacer(Modifier.height(14.dp))
                    Campo("Confirmar contraseña", confirmar, { confirmar = it }, password = true)
                }
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        when {
                            nombre.isBlank() || pass.isBlank() -> vm.limpiarError()
                            pass.length < 4 -> vm.limpiarError()
                            modo == "registro" && pass != confirmar -> vm.limpiarError()
                            modo == "login" -> vm.login(nombre, pass)
                            else -> vm.registro(nombre, pass)
                        }
                    },
                    enabled = !ui.cargando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = ShapeInput,
                    colors = ButtonDefaults.buttonColors(containerColor = Azul)
                ) {
                    Text(
                        if (modo == "login") "Entrar" else "Crear cuenta",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TabBtn(
    txt: String,
    activo: Boolean,
    mod: Modifier,
    onClick: () -> Unit
) {
    Box(
        mod.height(36.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (activo) Color.White else Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        Alignment.Center
    ) {
        Text(
            txt,
            color = if (activo) Azul else TextoSuave,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp
        )
    }
}

@Composable
private fun Campo(
    label: String,
    valor: String,
    onChange: (String) -> Unit,
    password: Boolean = false
) {
    Column {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextoSuave)
        Spacer(Modifier.height(6.dp))
        var focus by remember { mutableStateOf(false) }
        BasicTextField(
            value = valor,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.5.sp, color = Texto),
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focus) Color.White else Fondo, ShapeInput)
                .border(1.dp, if (focus) Azul else Borde, ShapeInput)
                .padding(12.dp, 14.dp)
                .onFocusChanged { focus = it.isFocused }
        )
    }
}
