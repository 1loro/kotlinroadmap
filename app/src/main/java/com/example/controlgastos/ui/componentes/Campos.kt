package com.example.controlgastos.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.ui.theme.Gastos

/** Etiqueta de un campo, con texto opcional a la derecha ("Mínimo 8 caracteres"). */
@Composable
fun Etiqueta(texto: String, aparte: String? = null) {
    val c = Gastos.colores
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, modifier = Modifier.weight(1f), style = estilo(15.sp, FontWeight.Medium, color = c.fg))
        if (aparte != null) Text(aparte, style = estilo(13.sp, color = c.muted))
    }
}

/**
 * Campo de texto (`.field` de la web): fondo gris, borde rojo al enfocar.
 * `etiqueta` también se usa como descripción para accesibilidad y tests.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    teclado: KeyboardOptions = KeyboardOptions.Default,
    contrasena: Boolean = false,
    largoMaximo: Int = Int.MAX_VALUE,
) {
    val c = Gastos.colores
    var enfocado by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(forma)
            .background(if (enfocado) c.surface else c.subtle)
            .border(1.dp, if (enfocado) c.accent else c.line, forma)
            .padding(start = 16.dp, end = if (contrasena) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (valor.isEmpty()) Text(placeholder, style = estilo(16.sp, color = c.muted))
            BasicTextField(
                value = valor,
                onValueChange = { if (it.length <= largoMaximo) onCambio(it) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { enfocado = it.isFocused }
                    .semantics { contentDescription = etiqueta },
                singleLine = true,
                textStyle = estilo(16.sp, color = c.fg),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = teclado,
                visualTransformation = if (contrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            )
        }
        if (contrasena) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).clickable { visible = !visible }
                    .semantics { contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña" },
                Alignment.Center,
            ) { GIcono(if (visible) R.drawable.lucide_eye_off else R.drawable.lucide_eye, tamano = 18.dp, color = c.muted) }
        }
    }
}

/** Selector desplegable con el aspecto de un campo (Moneda, Zona horaria). */
@Composable
fun CampoSeleccion(valor: String, opciones: List<Pair<String, String>>, onElegir: (String) -> Unit, etiqueta: String) {
    val c = Gastos.colores
    var abierto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(16.dp)
    Box {
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(forma).background(c.subtle).border(1.dp, c.line, forma)
                .clickable { abierto = true }.padding(horizontal = 16.dp).semantics { contentDescription = etiqueta },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(opciones.firstOrNull { it.first == valor }?.second ?: valor, style = estilo(16.sp, color = c.fg), maxLines = 1)
            GIcono(R.drawable.lucide_chevron_right, tamano = 16.dp, color = c.muted)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (clave, texto) ->
                DropdownMenuItem(
                    text = { Text(texto, style = estilo(15.sp, if (clave == valor) FontWeight.SemiBold else FontWeight.Normal)) },
                    onClick = {
                        onElegir(clave)
                        abierto = false
                    },
                )
            }
        }
    }
}

/** Grupo con título de sección de formulario ("Categoría", "Fecha"). */
@Composable
fun GrupoFormulario(titulo: String, aparte: String? = null, contenido: @Composable () -> Unit) {
    val c = Gastos.colores
    Column {
        Row(Modifier.padding(bottom = 12.dp)) {
            Text(titulo, style = estilo(15.sp, FontWeight.SemiBold, color = c.fg))
            if (aparte != null) Text(" $aparte", style = estilo(15.sp, color = c.muted))
        }
        contenido()
    }
}
