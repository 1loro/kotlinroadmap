package com.example.controlgastos.ui.cotizaciones

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.ui.formatearFechaHora
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.Instant

@Composable
fun CotizacionesRuta(viewModel: CotizacionesViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    CotizacionesScreen(estado, onActualizar = viewModel::actualizar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizacionesScreen(estado: CotizacionesUiState, onActualizar: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dólar hoy") },
                actions = {
                    IconButton(onClick = onActualizar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (estado) {
                CotizacionesUiState.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is CotizacionesUiState.Error -> ErrorSinDatos(estado.mensaje, onActualizar, Modifier.align(Alignment.Center))

                is CotizacionesUiState.Exito -> PullToRefreshBox(
                    isRefreshing = estado.actualizando,
                    onRefresh = onActualizar,
                ) {
                    ListaCotizaciones(estado)
                }
            }
        }
    }
}

@Composable
private fun ListaCotizaciones(estado: CotizacionesUiState.Exito) {
    Column(Modifier.fillMaxSize()) {
        // El aviso va FUERA de la lista, fijo arriba. Dentro del LazyColumn
        // aparecía oculto: al insertarse como primer ítem, la lista conserva
        // la posición de la tarjeta que ya mostraba (la sigue por su `key`).
        estado.aviso?.let { aviso ->
            AvisoDatosGuardados(
                aviso,
                estado.cotizaciones.maxOfOrNull { it.actualizada },
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
        }
        Cotizaciones(estado.cotizaciones)
    }
}

@Composable
private fun Cotizaciones(cotizaciones: List<Cotizacion>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cotizaciones.isEmpty()) {
            item { Text("No hay cotizaciones disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(cotizaciones, key = { it.casa }) { TarjetaCotizacion(it) }
        item {
            Text(
                "Fuente: dolarapi.com · Desliza hacia abajo para actualizar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TarjetaCotizacion(cotizacion: Cotizacion) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(cotizacion.nombre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Precio("Compra", cotizacion.compra, Modifier.weight(1f))
                Precio("Venta", cotizacion.venta, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Actualizado: ${formatearFechaHora(cotizacion.actualizada)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Precio(titulo: String, valor: Double?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.labelMedium)
        Text(
            valor?.let(::formatearMonto) ?: "—",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AvisoDatosGuardados(mensaje: String, ultimaFecha: Instant?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Column {
                Text(mensaje, fontWeight = FontWeight.SemiBold)
                Text(
                    "Mostrando datos guardados" + (ultimaFecha?.let { " del ${formatearFechaHora(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ErrorSinDatos(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(mensaje, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "No hay cotizaciones guardadas todavía. Revisa tu conexión e intenta de nuevo.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

private val ejemplo = listOf(
    Cotizacion("oficial", "Oficial", 1510.0, 1560.0, Instant.parse("2026-10-26T16:00:00Z")),
    Cotizacion("blue", "Blue", 1555.0, 1575.0, Instant.parse("2026-10-26T19:00:00Z")),
)

@Preview(showBackground = true)
@Composable
private fun ExitoConAvisoPreview() {
    ControlGastosTheme {
        CotizacionesScreen(CotizacionesUiState.Exito(ejemplo, aviso = "Sin conexión a internet"), {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() {
    ControlGastosTheme { CotizacionesScreen(CotizacionesUiState.Error("Sin conexión a internet"), {}) }
}
