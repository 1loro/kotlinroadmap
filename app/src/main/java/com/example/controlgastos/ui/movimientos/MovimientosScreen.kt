package com.example.controlgastos.ui.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.data.DatosDeEjemplo
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.model.TipoMovimiento
import com.example.controlgastos.ui.componentes.MovimientoItem
import com.example.controlgastos.ui.theme.ControlGastosTheme

enum class Filtro(val etiqueta: String, val tipo: TipoMovimiento?) {
    TODOS("Todos", null),
    INGRESOS("Ingresos", TipoMovimiento.INGRESO),
    GASTOS("Gastos", TipoMovimiento.GASTO),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    movimientos: List<Movimiento>,
    onNuevoMovimiento: () -> Unit,
    onMovimientoClick: (Long) -> Unit,
) {
    // rememberSaveable (a diferencia de remember) sobrevive a la rotación,
    // porque guarda el valor en el Bundle de la Activity. Sirve para valores
    // simples como este enum.
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    val visibles = movimientos.filter { filtro.tipo == null || it.tipo == filtro.tipo }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Movimientos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoMovimiento) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo movimiento")
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filtro.entries.forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta) },
                    )
                }
            }

            if (visibles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay movimientos para mostrar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // LazyColumn solo compone los elementos visibles: eficiente
                // aunque haya miles. `key` ayuda a Compose a seguir cada ítem
                // cuando la lista cambia (agregar/eliminar/filtrar).
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(visibles, key = { it.id }) { movimiento ->
                        MovimientoItem(
                            movimiento,
                            onClick = { onMovimientoClick(movimiento.id) },
                            modifier = Modifier.animateItem(),
                        )
                        HorizontalDivider(Modifier.padding(start = 80.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovimientosPreview() {
    ControlGastosTheme {
        MovimientosScreen(DatosDeEjemplo.movimientos(), {}, {})
    }
}
