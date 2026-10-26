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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.TipoMovimiento
import com.example.controlgastos.domain.usecase.ErrorValidacion
import com.example.controlgastos.ui.theme.ControlGastosTheme

@Composable
fun FormularioRuta(onCerrar: () -> Unit, viewModel: FormularioViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onCerrar()
    }

    FormularioScreen(
        estado = estado,
        onTipo = viewModel::cambiarTipo,
        onDescripcion = viewModel::cambiarDescripcion,
        onMonto = viewModel::cambiarMonto,
        onCategoria = viewModel::elegirCategoria,
        onGuardar = viewModel::guardar,
        onCancelar = onCerrar,
    )
}

/** Textos de error para el usuario. La regla vive en el dominio; el texto, en la UI. */
private val ErrorValidacion.mensaje: String
    get() = when (this) {
        ErrorValidacion.DESCRIPCION_VACIA -> "Escribe una descripción"
        ErrorValidacion.MONTO_INVALIDO -> "Ingresa un monto mayor que 0"
        ErrorValidacion.SIN_CATEGORIA -> "Elige una categoría"
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormularioScreen(
    estado: FormularioUiState,
    onTipo: (TipoMovimiento) -> Unit,
    onDescripcion: (String) -> Unit,
    onMonto: (String) -> Unit,
    onCategoria: (Categoria) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    // Helper local: el mensaje de un error si está presente, o null.
    fun error(tipo: ErrorValidacion): String? = tipo.takeIf { it in estado.errores }?.mensaje

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
                        selected = estado.tipo == opcion,
                        onClick = { onTipo(opcion) },
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoMovimiento.entries.size),
                    ) {
                        Text(if (opcion == TipoMovimiento.GASTO) "Gasto" else "Ingreso")
                    }
                }
            }

            val errorDescripcion = error(ErrorValidacion.DESCRIPCION_VACIA)
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = onDescripcion,
                label = { Text("Descripción") },
                singleLine = true,
                isError = errorDescripcion != null,
                supportingText = errorDescripcion?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            val errorMonto = error(ErrorValidacion.MONTO_INVALIDO)
            OutlinedTextField(
                value = estado.montoTexto,
                onValueChange = onMonto,
                label = { Text("Monto") },
                prefix = { Text("$ ") },
                singleLine = true,
                isError = errorMonto != null,
                supportingText = errorMonto?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.categoriasDisponibles.forEach { opcion ->
                    FilterChip(
                        selected = estado.categoria == opcion,
                        onClick = { onCategoria(opcion) },
                        label = { Text("${opcion.emoji} ${opcion.etiqueta}") },
                    )
                }
            }
            error(ErrorValidacion.SIN_CATEGORIA)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = onGuardar,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Guardar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormularioConErroresPreview() {
    ControlGastosTheme {
        FormularioScreen(
            estado = FormularioUiState(
                descripcion = "Cine",
                errores = setOf(ErrorValidacion.MONTO_INVALIDO, ErrorValidacion.SIN_CATEGORIA),
            ),
            onTipo = {}, onDescripcion = {}, onMonto = {}, onCategoria = {}, onGuardar = {}, onCancelar = {},
        )
    }
}
