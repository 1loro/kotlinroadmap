package com.example.controlgastos.ui.indicadores

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
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.ui.formatearDiaChile
import com.example.controlgastos.ui.formatearValorIndicador
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.Instant

@Composable
fun IndicadoresRuta(viewModel: IndicadoresViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    IndicadoresScreen(estado, onActualizar = viewModel::actualizar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicadoresScreen(estado: IndicadoresUiState, onActualizar: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indicadores") },
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
                IndicadoresUiState.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is IndicadoresUiState.Error -> ErrorSinDatos(estado.mensaje, onActualizar, Modifier.align(Alignment.Center))

                is IndicadoresUiState.Exito -> PullToRefreshBox(
                    isRefreshing = estado.actualizando,
                    onRefresh = onActualizar,
                ) {
                    ListaIndicadores(estado)
                }
            }
        }
    }
}

@Composable
private fun ListaIndicadores(estado: IndicadoresUiState.Exito) {
    Column(Modifier.fillMaxSize()) {
        // El aviso va FUERA de la lista, fijo arriba. Dentro del LazyColumn
        // aparecía oculto: al insertarse como primer ítem, la lista conserva
        // la posición de la tarjeta que ya mostraba (la sigue por su `key`).
        estado.aviso?.let { aviso ->
            AvisoDatosGuardados(
                aviso,
                estado.indicadores.maxOfOrNull { it.fecha },
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
        }
        Indicadores(estado.indicadores)
    }
}

@Composable
private fun Indicadores(indicadores: List<Indicador>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (indicadores.isEmpty()) {
            item { Text("No hay indicadores disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(indicadores, key = { it.codigo }) { TarjetaIndicador(it) }
        item {
            Text(
                "Fuente: mindicador.cl (Banco Central de Chile) · Desliza hacia abajo para actualizar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TarjetaIndicador(indicador: Indicador) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(indicador.nombre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                formatearValorIndicador(indicador.valor),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Valor del ${formatearDiaChile(indicador.fecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                    "Mostrando datos guardados" + (ultimaFecha?.let { " del ${formatearDiaChile(it)}" } ?: ""),
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
            "No hay indicadores guardados todavía. Revisa tu conexión e intenta de nuevo.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

private val ejemplo = listOf(
    Indicador("dolar", "Dólar observado", 990.35, Instant.parse("2026-10-26T03:00:00Z")),
    Indicador("uf", "Unidad de fomento (UF)", 41221.08, Instant.parse("2026-10-26T03:00:00Z")),
)

@Preview(showBackground = true)
@Composable
private fun ExitoConAvisoPreview() {
    ControlGastosTheme {
        IndicadoresScreen(IndicadoresUiState.Exito(ejemplo, aviso = "Sin conexión a internet"), {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() {
    ControlGastosTheme { IndicadoresScreen(IndicadoresUiState.Error("Sin conexión a internet"), {}) }
}
