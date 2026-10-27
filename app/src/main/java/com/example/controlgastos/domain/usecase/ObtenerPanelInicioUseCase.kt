package com.example.controlgastos.domain.usecase

import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.model.Resumen
import com.example.controlgastos.domain.model.resumen
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.MovimientosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Todo lo que muestra la pantalla de Inicio, ya calculado. */
data class PanelInicio(
    val resumen: Resumen,
    val ultimos: List<Movimiento>,
    /** null si no hay valor del dólar disponible (sin conexión y sin caché). */
    val saldoEnDolares: Double?,
)

/**
 * Combina dos fuentes de datos (movimientos y indicadores) en un solo
 * resultado. `combine` emite de nuevo cada vez que CUALQUIERA cambia.
 */
class ObtenerPanelInicioUseCase @Inject constructor(
    private val movimientos: MovimientosRepository,
    private val indicadores: IndicadoresRepository,
) {
    operator fun invoke(cantidadUltimos: Int = 5): Flow<PanelInicio> =
        combine(movimientos.movimientos, indicadores.indicadores) { lista, indicadores ->
            val resumen = lista.resumen()
            val dolar = indicadores.find { it.codigo == Indicador.DOLAR }?.valor
            PanelInicio(
                resumen = resumen,
                ultimos = lista.take(cantidadUltimos),
                saldoEnDolares = dolar?.takeIf { it > 0 }?.let { resumen.saldo / it },
            )
        }
}
