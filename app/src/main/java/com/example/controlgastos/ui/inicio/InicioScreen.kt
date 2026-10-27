package com.example.controlgastos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.data.DatosDeEjemplo
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Resumen
import com.example.controlgastos.domain.model.resumen
import com.example.controlgastos.domain.usecase.PanelInicio
import com.example.controlgastos.ui.componentes.MovimientoItem
import com.example.controlgastos.ui.componentes.colorMonto
import com.example.controlgastos.ui.formatearDolares
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.theme.ControlGastosTheme

/**
 * "Ruta": obtiene el ViewModel (Hilt lo crea con sus dependencias) y conecta
 * su estado con la pantalla. La pantalla en sí no sabe que existe un
 * ViewModel: recibe datos y avisa eventos, así se previsualiza y testea fácil.
 */
@Composable
fun InicioRuta(
    onNuevoMovimiento: () -> Unit,
    onVerTodos: () -> Unit,
    onMovimientoClick: (Long) -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    InicioScreen(estado, onNuevoMovimiento, onVerTodos, onMovimientoClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    estado: InicioUiState,
    onNuevoMovimiento: () -> Unit,
    onVerTodos: () -> Unit,
    onMovimientoClick: (Long) -> Unit,
) {
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
        val panel = (estado as? InicioUiState.Listo)?.panel
        if (panel == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val resumen = panel.resumen
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 88.dp, // espacio para el FAB
            ),
        ) {
            item { TarjetaSaldo(resumen, panel.saldoEnDolares, Modifier.padding(16.dp)) }

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
            items(panel.ultimos, key = { it.id }) { movimiento ->
                MovimientoItem(movimiento, onClick = { onMovimientoClick(movimiento.id) })
            }
        }
    }
}

@Composable
private fun TarjetaSaldo(resumen: Resumen, saldoEnDolares: Double?, modifier: Modifier = Modifier) {
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
            // null si no hay valor del dólar (sin conexión y sin caché): se omite.
            saldoEnDolares?.let { dolares ->
                Text(
                    "≈ ${formatearDolares(dolares)} al dólar observado",
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
        val movimientos = DatosDeEjemplo.movimientos()
        val panel = PanelInicio(movimientos.resumen(), movimientos.take(5), saldoEnDolares = 752.06)
        InicioScreen(InicioUiState.Listo(panel), {}, {}, {})
    }
}
