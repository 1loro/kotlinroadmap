package com.example.controlgastos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.ui.componentes.BotonIcono
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.CampoTexto
import com.example.controlgastos.ui.componentes.Etiqueta
import com.example.controlgastos.ui.componentes.Logo
import com.example.controlgastos.ui.componentes.MensajeError
import com.example.controlgastos.ui.componentes.TarjetaElevada
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.theme.Gastos

// Pantallas de login y registro (src/components/auth-shell.tsx y auth-forms.tsx).

/** Marco común: tema arriba a la derecha, logo grande y la tarjeta del formulario. */
@Composable
private fun MarcoAuth(titulo: String, descripcion: String, onTema: () -> Unit, contenido: @Composable () -> Unit) {
    val c = Gastos.colores
    Column(
        Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.CenterEnd) {
            BotonIcono(
                if (c.esOscuro) R.drawable.lucide_sun else R.drawable.lucide_moon,
                if (c.esOscuro) "Activar modo claro" else "Activar modo oscuro",
                onTema,
            )
        }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Logo(grande = true) }
        Spacer(Modifier.height(32.dp))
        TarjetaElevada(relleno = 24.dp) {
            Text(titulo, style = estilo(30.sp, FontWeight.SemiBold, (-0.05).em, c.fg))
            Text(
                descripcion,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
                style = estilo(16.sp, color = c.muted, interlineado = 26.sp),
            )
            contenido()
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EnlaceInferior(pregunta: String, accion: String, onClick: () -> Unit) {
    val c = Gastos.colores
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = c.muted)) { append("$pregunta ") }
            withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Medium)) { append(accion) }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        style = estilo(15.sp).copy(textAlign = TextAlign.Center),
    )
}

@Composable
fun LoginScreen(onIrARegistro: () -> Unit, onTema: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MarcoAuth("Inicia sesión", "Registra, ordena y compara tus gastos mes a mes.", onTema) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Etiqueta("Email")
                CampoTexto(
                    estado.email, viewModel::cambiarEmail, "Email", placeholder = "tu@email.com",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
            }
            Column {
                Etiqueta("Contraseña")
                CampoTexto(
                    estado.contrasena, viewModel::cambiarContrasena, "Contraseña", placeholder = "••••••••",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    contrasena = true,
                )
            }
            estado.error?.let { MensajeError(it) }
            BotonPrimario("Iniciar sesión", viewModel::iniciarSesion, Modifier.fillMaxWidth(), alto = 52.dp, cargando = estado.enviando)
            EnlaceInferior("¿No tienes cuenta?", "Regístrate", onIrARegistro)
        }
    }
}

@Composable
fun RegistroScreen(onIrALogin: () -> Unit, onTema: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MarcoAuth("Crea tu cuenta", "La misma cuenta sirve en la web, en el iPhone y aquí.", onTema) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Etiqueta("Nombre")
                CampoTexto(
                    estado.nombre, viewModel::cambiarNombre, "Nombre", placeholder = "Cómo quieres que te llamemos",
                    teclado = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            Column {
                Etiqueta("Email")
                CampoTexto(
                    estado.email, viewModel::cambiarEmail, "Email", placeholder = "tu@email.com",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
            }
            Column {
                Etiqueta("Contraseña", aparte = "Mínimo 8 caracteres")
                CampoTexto(
                    estado.contrasena, viewModel::cambiarContrasena, "Contraseña", placeholder = "••••••••",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    contrasena = true,
                )
            }
            estado.error?.let { MensajeError(it) }
            BotonPrimario("Crear cuenta", viewModel::registrarse, Modifier.fillMaxWidth(), alto = 52.dp, cargando = estado.enviando)
            EnlaceInferior("¿Ya tienes cuenta?", "Inicia sesión", onIrALogin)
        }
    }
}
