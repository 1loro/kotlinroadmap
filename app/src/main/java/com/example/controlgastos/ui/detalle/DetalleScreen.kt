package com.example.controlgastos.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.model.Categoria
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.ui.componentes.IconoCategoria
import com.example.controlgastos.ui.componentes.colorMonto
import com.example.controlgastos.ui.formatearFecha
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    movimiento: Movimiento?,
    // Room responde de forma asíncrona: mientras tanto `movimiento` es null
    // pero eso NO significa que no exista.
    cargando: Boolean,
    onVolver: () -> Unit,
    onEliminar: (Long) -> Unit,
) {
    var confirmarBorrado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (movimiento != null) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (movimiento == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                if (cargando) CircularProgressIndicator() else Text("El movimiento no existe")
            }
            return@Scaffold
        }

        Column(Modifier.padding(innerPadding).fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconoCategoria(movimiento.categoria, Modifier.size(64.dp))
                Text(movimiento.descripcion, style = MaterialTheme.typography.titleLarge)
                Text(
                    (if (movimiento.esGasto) "-" else "+") + formatearMonto(movimiento.monto),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colorMonto(movimiento.esGasto),
                )
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(if (movimiento.esGasto) "Gasto" else "Ingreso") },
                overlineContent = { Text("Tipo") },
            )
            ListItem(
                headlineContent = { Text(movimiento.categoria.etiqueta) },
                overlineContent = { Text("Categoría") },
            )
            ListItem(
                headlineContent = { Text(formatearFecha(movimiento.fecha)) },
                overlineContent = { Text("Fecha") },
            )
        }
    }

    if (confirmarBorrado && movimiento != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("\"${movimiento.descripcion}\" se eliminará definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    onEliminar(movimiento.id)
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetallePreview() {
    ControlGastosTheme {
        DetalleScreen(Movimiento(1, "Supermercado", 95_400.0, Categoria.COMIDA, LocalDate.now()), cargando = false, {}, {})
    }
}
