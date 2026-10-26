package com.example.controlgastos.ui.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.model.Categoria
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.model.TipoMovimiento
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.LocalDate

/**
 * Convierte el texto del campo monto ("1500,50" o "1500.50") a número.
 * Devuelve null si no es un monto válido y mayor que cero.
 */
fun parsearMonto(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormularioScreen(
    onGuardar: (Movimiento) -> Unit,
    onCancelar: () -> Unit,
) {
    // Estado del formulario. rememberSaveable conserva lo escrito al rotar.
    var tipo by rememberSaveable { mutableStateOf(TipoMovimiento.GASTO) }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var montoTexto by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }

    // Valores derivados: se recalculan en cada recomposición.
    val monto = parsearMonto(montoTexto)
    val descripcionValida = descripcion.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo movimiento") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoMovimiento.entries.forEachIndexed { indice, opcion ->
                    SegmentedButton(
                        selected = tipo == opcion,
                        onClick = {
                            tipo = opcion
                            // La categoría elegida puede no corresponder al nuevo tipo.
                            if (categoria?.tipo != opcion) categoria = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoMovimiento.entries.size),
                    ) {
                        Text(if (opcion == TipoMovimiento.GASTO) "Gasto" else "Ingreso")
                    }
                }
            }

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                singleLine = true,
                isError = intentoGuardar && !descripcionValida,
                supportingText = if (intentoGuardar && !descripcionValida) {
                    { Text("Escribe una descripción") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = montoTexto,
                onValueChange = { nuevo ->
                    // Solo dígitos y un separador decimal.
                    if (nuevo.count { it == ',' || it == '.' } <= 1 && nuevo.all { it.isDigit() || it == ',' || it == '.' }) {
                        montoTexto = nuevo
                    }
                },
                label = { Text("Monto") },
                prefix = { Text("$ ") },
                singleLine = true,
                isError = intentoGuardar && monto == null,
                supportingText = if (intentoGuardar && monto == null) {
                    { Text("Ingresa un monto mayor que 0") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categoria.de(tipo).forEach { opcion ->
                    FilterChip(
                        selected = categoria == opcion,
                        onClick = { categoria = opcion },
                        label = { Text("${opcion.emoji} ${opcion.etiqueta}") },
                    )
                }
            }
            if (intentoGuardar && categoria == null) {
                Text(
                    "Elige una categoría",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    intentoGuardar = true
                    // Copia local: `categoria` es una propiedad delegada (by) y el
                    // compilador no puede hacer smart cast sobre ella; sobre un val sí.
                    val categoriaElegida = categoria
                    if (descripcionValida && monto != null && categoriaElegida != null) {
                        onGuardar(
                            Movimiento(
                                id = 0, // el estado asigna el id real
                                descripcion = descripcion.trim(),
                                monto = monto,
                                categoria = categoriaElegida,
                                fecha = LocalDate.now(),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Guardar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormularioPreview() {
    ControlGastosTheme { FormularioScreen({}, {}) }
}
