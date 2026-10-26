package com.example.controlgastos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.data.DatosDeEjemplo
import com.example.controlgastos.model.Categoria
import com.example.controlgastos.model.Cotizacion
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.model.Resumen
import com.example.controlgastos.model.resumen
import com.example.controlgastos.ui.componentes.MovimientoItem
import com.example.controlgastos.ui.componentes.colorMonto
import com.example.controlgastos.ui.formatearDolares
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.Instant

private const val ULTIMOS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    movimientos: List<Movimiento>,
    // Puede ser null (sin conexión y sin caché): la tarjeta lo omite.
    dolarOficial: Cotizacion?,
    onNuevoMovimiento: () -> Unit,
    onVerTodos: () -> Unit,
    onMovimientoClick: (Long) -> Unit,
) {
    val resumen = movimientos.resumen()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Control de Gastos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoMovimiento,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo movimiento") },
                text = { Text("Nuevo") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 88.dp, // espacio para el FAB
            ),
        ) {
            item { TarjetaSaldo(resumen, dolarOficial, Modifier.padding(16.dp)) }

            if (resumen.gastoPorCategoria.isNotEmpty()) {
                item { TituloSeccion("Gastos por categoría") }
                items(resumen.gastoPorCategoria, key = { it.first.name }) { (categoria, total) ->
                    BarraCategoria(categoria, total, resumen.gastos)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TituloSeccion("Últimos movimientos", Modifier.weight(1f))
                    TextButton(onClick = onVerTodos) { Text("Ver todos") }
                }
            }
            items(movimientos.take(ULTIMOS), key = { it.id }) { movimiento ->
                MovimientoItem(movimiento, onClick = { onMovimientoClick(movimiento.id) })
            }
        }
    }
}

@Composable
private fun TarjetaSaldo(resumen: Resumen, dolarOficial: Cotizacion?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Saldo disponible", style = MaterialTheme.typography.labelLarge)
            Text(
                formatearMonto(resumen.saldo),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            dolarOficial?.venta?.takeIf { it > 0 }?.let { venta ->
                Text(
                    "≈ ${formatearDolares(resumen.saldo / venta)} al dólar oficial",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DatoResumen("Ingresos", resumen.ingresos, esGasto = false)
                DatoResumen("Gastos", resumen.gastos, esGasto = true)
            }
        }
    }
}

@Composable
private fun DatoResumen(titulo: String, monto: Double, esGasto: Boolean) {
    Column {
        Text(titulo, style = MaterialTheme.typography.labelMedium)
        Text(
            formatearMonto(monto),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorMonto(esGasto),
        )
    }
}

@Composable
private fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun BarraCategoria(categoria: Categoria, total: Double, totalGastos: Double) {
    val fraccion = if (totalGastos > 0) (total / totalGastos).toFloat() else 0f
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("${categoria.emoji}  ${categoria.etiqueta}", Modifier.weight(1f))
            Text(formatearMonto(total), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraccion },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun InicioPreview() {
    ControlGastosTheme {
        InicioScreen(
            DatosDeEjemplo.movimientos(),
            Cotizacion(Cotizacion.OFICIAL, "Oficial", 1510.0, 1560.0, Instant.now()),
            {}, {}, {},
        )
    }
}
