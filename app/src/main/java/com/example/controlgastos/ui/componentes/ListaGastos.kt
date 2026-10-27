package com.example.controlgastos.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.domain.etiquetaDia
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.ui.formatearDinero
import com.example.controlgastos.ui.theme.Gastos
import java.time.LocalDate

// Lista de gastos de la web (src/components/expense-list.tsx).

/**
 * @param agrupada true: por día con su total ("Hoy · $12.500"); false: la
 *   fecha va en cada fila (Movimientos recientes del Inicio).
 */
@Composable
fun ListaGastos(gastos: List<Gasto>, moneda: String, hoy: LocalDate, agrupada: Boolean = true, onClick: (Gasto) -> Unit) {
    val c = Gastos.colores
    val grupos = if (agrupada) gastos.groupBy { it.fecha } else mapOf(hoy to gastos)
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        grupos.forEach { (dia, filas) ->
            Column {
                if (agrupada) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text(etiquetaDia(dia, hoy), modifier = Modifier.weight(1f), style = estilo(13.sp, FontWeight.Medium, color = c.muted))
                        Text(formatearDinero(filas.sumOf { it.monto }, moneda), style = estilo(13.sp, color = c.muted, tabular = true))
                    }
                }
                filas.forEachIndexed { i, gasto ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    FilaMovimiento(
                        icono = gasto.categoriaIcono,
                        titulo = gasto.nota ?: gasto.categoriaNombre,
                        subtitulo = (if (gasto.nota != null) gasto.categoriaNombre else "Sin nota") +
                            if (!agrupada) " · ${etiquetaDia(gasto.fecha, hoy)}" else "",
                        monto = formatearDinero(gasto.monto, moneda),
                        onClick = { onClick(gasto) },
                    )
                }
            }
        }
    }
}

/** Ingresos locales: en verde y con "+". */
@Composable
fun ListaIngresos(ingresos: List<Ingreso>, moneda: String, hoy: LocalDate, onClick: (Ingreso) -> Unit) {
    val c = Gastos.colores
    Column {
        ingresos.forEachIndexed { i, ingreso ->
            if (i > 0) HorizontalDivider(color = c.line)
            FilaMovimiento(
                icono = ingreso.tipo.icono,
                titulo = ingreso.nota ?: ingreso.tipo.etiqueta,
                subtitulo = "${if (ingreso.nota != null) ingreso.tipo.etiqueta else "Ingreso"} · ${etiquetaDia(ingreso.fecha, hoy)}",
                monto = "+${formatearDinero(ingreso.monto, moneda)}",
                colorAcento = c.positive,
                onClick = { onClick(ingreso) },
            )
        }
    }
}

@Composable
private fun FilaMovimiento(
    icono: String,
    titulo: String,
    subtitulo: String,
    monto: String,
    colorAcento: Color = Gastos.colores.accent,
    onClick: () -> Unit,
) {
    val c = Gastos.colores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CajaCategoria(icono, colorAcento = colorAcento)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = estilo(15.sp, FontWeight.Medium, color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitulo, style = estilo(13.sp, color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(monto, style = estilo(15.sp, FontWeight.SemiBold, color = if (colorAcento == c.accent) c.fg else colorAcento, tabular = true))
        GIcono(R.drawable.lucide_chevron_right, tamano = 16.dp, color = c.muted.copy(alpha = 0.6f))
    }
}
