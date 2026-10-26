package com.example.controlgastos.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.ui.formatearFecha
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.theme.ControlGastosTheme
import java.time.LocalDate

@Composable
fun MovimientoItem(movimiento: Movimiento, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // 72dp de alto total: objetivo táctil cómodo (mínimo 48dp).
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconoCategoria(movimiento.categoria)
        Column(Modifier.weight(1f)) {
            Text(
                movimiento.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${movimiento.categoria.etiqueta} · ${formatearFecha(movimiento.fecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MontoConSigno(movimiento)
    }
}

@Composable
fun IconoCategoria(categoria: Categoria, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(categoria.emoji, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun MontoConSigno(movimiento: Movimiento, modifier: Modifier = Modifier) {
    val signo = if (movimiento.esGasto) "-" else "+"
    Text(
        "$signo${formatearMonto(movimiento.monto)}",
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = colorMonto(movimiento.esGasto),
    )
}

@Composable
fun colorMonto(esGasto: Boolean): Color =
    if (esGasto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Preview(showBackground = true)
@Composable
private fun MovimientoItemPreview() {
    ControlGastosTheme {
        Column {
            MovimientoItem(Movimiento(1, "Supermercado", 95_400.0, Categoria.COMIDA, LocalDate.now()), onClick = {})
            MovimientoItem(Movimiento(2, "Sueldo", 850_000.0, Categoria.SUELDO, LocalDate.now()), onClick = {})
        }
    }
}
