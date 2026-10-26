package com.example.controlgastos.domain.usecase

import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.model.Resumen
import com.example.controlgastos.domain.model.resumen
import com.example.controlgastos.domain.repository.CotizacionesRepository
import com.example.controlgastos.domain.repository.MovimientosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Todo lo que muestra la pantalla de Inicio, ya calculado. */
data class PanelInicio(
    val resumen: Resumen,
    val ultimos: List<Movimiento>,
    /** null si no hay cotización oficial disponible (sin conexión y sin caché). */
    val saldoEnDolares: Double?,
)

/**
 * Combina dos fuentes de datos (movimientos y cotizaciones) en un solo
 * resultado. `combine` emite de nuevo cada vez que CUALQUIERA cambia.
 */
class ObtenerPanelInicioUseCase @Inject constructor(
    private val movimientos: MovimientosRepository,
    private val cotizaciones: CotizacionesRepository,
) {
    operator fun invoke(cantidadUltimos: Int = 5): Flow<PanelInicio> =
        combine(movimientos.movimientos, cotizaciones.cotizaciones) { lista, cotizaciones ->
            val resumen = lista.resumen()
            val ventaOficial = cotizaciones.find { it.casa == Cotizacion.OFICIAL }?.venta
            PanelInicio(
                resumen = resumen,
                ultimos = lista.take(cantidadUltimos),
                saldoEnDolares = ventaOficial?.takeIf { it > 0 }?.let { resumen.saldo / it },
            )
        }
}
