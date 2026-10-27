package com.example.controlgastos.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.domain.fraseResumen
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.nombreMesCorto
import com.example.controlgastos.ui.componentes.Aviso
import com.example.controlgastos.ui.componentes.BarrasMeses
import com.example.controlgastos.ui.componentes.BotonContorno
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.DesgloseCategorias
import com.example.controlgastos.ui.componentes.Divisor
import com.example.controlgastos.ui.componentes.EspacioSeccion
import com.example.controlgastos.ui.componentes.Esqueleto
import com.example.controlgastos.ui.componentes.EstadoVacio
import com.example.controlgastos.ui.componentes.ListaGastos
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.Pastilla
import com.example.controlgastos.ui.componentes.Tarjeta
import com.example.controlgastos.ui.componentes.TarjetaElevada
import com.example.controlgastos.ui.componentes.Titular
import com.example.controlgastos.ui.componentes.TituloSeccion
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.formatearDinero
import com.example.controlgastos.ui.formatearDolares
import com.example.controlgastos.ui.formatearValorIndicador
import com.example.controlgastos.ui.theme.Gastos
import java.time.YearMonth
import kotlin.math.roundToInt

// Inicio de la web (src/app/(app)/page.tsx) + ingresos e indicadores.

@Composable
fun InicioRuta(
    onNuevoGasto: () -> Unit,
    onNuevoIngreso: () -> Unit,
    onVerMovimientos: () -> Unit,
    onComparar: () -> Unit,
    onIndicadores: () -> Unit,
    onGasto: (Long) -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    when (val e = estado) {
        InicioUiState.Cargando -> EsqueletoInicio()
        is InicioUiState.Listo -> Pantalla(e.actualizando, viewModel::actualizar) {
            e.aviso?.let { Aviso(it) }
            InicioContenido(e.panel, onNuevoGasto, onNuevoIngreso, onVerMovimientos, onComparar, onIndicadores, onGasto)
        }
    }
}

@Composable
private fun InicioContenido(
    p: PanelInicio,
    onNuevoGasto: () -> Unit,
    onNuevoIngreso: () -> Unit,
    onVerMovimientos: () -> Unit,
    onComparar: () -> Unit,
    onIndicadores: () -> Unit,
    onGasto: (Long) -> Unit,
) {
    val c = Gastos.colores
    val moneda = p.usuario.moneda
    val r = p.resumen
    val mes = YearMonth.from(p.hoy)

    // --- Encabezado: "Este mes llevas $X." ---
    Column(Modifier.padding(top = 32.dp, bottom = 32.dp)) {
        Pastilla("Hola, ${p.usuario.primerNombre} · ${r.cantidad} ${if (r.cantidad == 1) "movimiento" else "movimientos"} este mes")
        Spacer(Modifier.height(24.dp))
        Titular("Este mes llevas", acento = "${formatearDinero(r.total, moneda)}.", grande = true)
        Text(
            fraseResumen(r, p.hoy) { formatearDinero(it, moneda) },
            modifier = Modifier.padding(top = 24.dp),
            style = estilo(17.sp, color = c.muted, interlineado = 27.sp),
        )
    }

    TarjetaElevada {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Dato("Diario", formatearDinero(r.promedioDiario, moneda, compacto = true), Modifier.weight(1f))
            Separador()
            Dato("Proyección", formatearDinero(r.proyeccion, moneda, compacto = true), Modifier.weight(1f))
            Separador()
            Dato(
                "vs ${nombreMesCorto(mes.minusMonths(1)).lowercase()}",
                r.cambio?.let { "${if (it > 0) "+" else ""}${it.roundToInt()}%" } ?: "—",
                Modifier.weight(1f),
            )
        }
        BotonPrimario("Registrar gasto", onNuevoGasto, Modifier.fillMaxWidth().padding(top = 8.dp), icono = R.drawable.lucide_plus)
    }

    Spacer(Modifier.height(40.dp))
    Divisor()
    EspacioSeccion()

    TituloSeccion("Últimos 6 meses", "Tu evolución") {
        BotonContorno("Comparar", onComparar, iconoFinal = R.drawable.lucide_arrow_right)
    }
    Tarjeta { BarrasMeses(p.ultimosMeses, mes, moneda) }

    EspacioSeccion()
    TituloSeccion("Por categoría", "En qué se va")
    Tarjeta {
        if (p.porCategoria.isNotEmpty()) {
            DesgloseCategorias(p.porCategoria, r.total, moneda)
        } else {
            Text(
                "Aún no hay gastos este mes.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                style = estilo(14.sp, color = c.muted).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            )
        }
    }

    EspacioSeccion()
    TituloSeccion("Movimientos recientes", "Lo último") {
        if (p.recientes.isNotEmpty()) BotonContorno("Ver todo", onVerMovimientos, iconoFinal = R.drawable.lucide_arrow_right)
    }
    Tarjeta {
        if (p.recientes.isNotEmpty()) {
            ListaGastos(p.recientes, moneda, p.hoy, agrupada = false) { onGasto(it.id) }
        } else {
            EstadoVacio(R.drawable.lucide_wallet, "Sin movimientos todavía", "Registra tu primer gasto desde aquí o desde el Atajo del iPhone.") {
                BotonPrimario("Registrar gasto", onNuevoGasto, alto = 44.dp, icono = R.drawable.lucide_receipt_text)
            }
        }
    }

    // --- Extras de Android ---
    EspacioSeccion()
    TituloSeccion("Balance del mes", "Solo en este teléfono") {
        BotonContorno("Ingreso", onNuevoIngreso, icono = R.drawable.lucide_plus)
    }
    Tarjeta { Balance(p.ingresosMes, r.total, moneda) }

    if (p.indicadores.isNotEmpty()) {
        EspacioSeccion()
        TituloSeccion("Hoy en Chile", "Indicadores") {
            BotonContorno("Ver todo", onIndicadores, iconoFinal = R.drawable.lucide_arrow_right)
        }
        Tarjeta { MiniIndicadores(p.indicadores, r.total) }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    Column(modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = estilo(12.sp, color = c.muted), maxLines = 1)
        Text(
            valor,
            style = estilo(17.sp, FontWeight.SemiBold, (-0.02).em, c.fg, tabular = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Separador() = Box(Modifier.width(1.dp).fillMaxHeight().background(Gastos.colores.line))

@Composable
private fun Balance(ingresos: Double, gastos: Double, moneda: String) {
    val c = Gastos.colores
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilaBalance("Ingresos", "+${formatearDinero(ingresos, moneda)}", c.positive)
        FilaBalance("Gastos", "-${formatearDinero(gastos, moneda)}", c.fg)
        Divisor()
        val saldo = ingresos - gastos
        FilaBalance("Saldo", formatearDinero(saldo, moneda), if (saldo >= 0) c.positive else c.accent, fuerte = true)
        if (ingresos == 0.0) {
            Text(
                "Registra tus ingresos para ver cuánto te queda. Se guardan solo en este teléfono.",
                style = estilo(13.sp, color = c.muted, interlineado = 19.sp),
            )
        }
    }
}

@Composable
private fun FilaBalance(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color, fuerte: Boolean = false) {
    val c = Gastos.colores
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = estilo(15.sp, if (fuerte) FontWeight.SemiBold else FontWeight.Normal, color = c.muted))
        Text(valor, style = estilo(if (fuerte) 18.sp else 15.sp, FontWeight.SemiBold, color = color, tabular = true))
    }
}

/** Dólar, euro, UF y UTM en una cuadrícula, y lo gastado este mes en dólares. */
@Composable
private fun MiniIndicadores(indicadores: List<Indicador>, gastadoMes: Double) {
    val c = Gastos.colores
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        indicadores.chunked(2).forEach { fila ->
            Row(Modifier.fillMaxWidth()) {
                fila.forEach { ind ->
                    Column(Modifier.weight(1f)) {
                        Text(ind.nombreCorto, style = estilo(13.sp, color = c.muted))
                        Text(formatearValorIndicador(ind.valor), style = estilo(18.sp, FontWeight.SemiBold, (-0.02).em, c.fg, tabular = true))
                    }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        indicadores.find { it.codigo == Indicador.DOLAR }?.valor?.takeIf { it > 0 }?.let { dolar ->
            Text(
                "Este mes llevas ≈ ${formatearDolares(gastadoMes / dolar)} al dólar observado.",
                style = estilo(13.sp, color = c.muted),
            )
        }
    }
}

private val Indicador.nombreCorto: String
    get() = when (codigo) {
        "dolar" -> "Dólar"
        "euro" -> "Euro"
        "uf" -> "UF"
        "utm" -> "UTM"
        else -> nombre
    }

@Composable
private fun EsqueletoInicio() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Esqueleto(Modifier.width(256.dp).height(32.dp))
        Esqueleto(Modifier.fillMaxWidth().height(96.dp))
        Esqueleto(Modifier.fillMaxWidth().height(48.dp), radio = 12.dp)
        Esqueleto(Modifier.fillMaxWidth().height(144.dp))
        Esqueleto(Modifier.fillMaxWidth().height(288.dp))
    }
}
