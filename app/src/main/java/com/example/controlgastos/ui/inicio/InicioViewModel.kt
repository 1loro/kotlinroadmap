package com.example.controlgastos.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.ResumenMes
import com.example.controlgastos.domain.hoyEn
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.TotalCategoria
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.SesionRepository
import com.example.controlgastos.domain.resumenMes
import com.example.controlgastos.domain.totalesPorCategoria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Todo lo que muestra el Inicio, ya calculado. */
data class PanelInicio(
    val usuario: Usuario,
    val hoy: LocalDate,
    val resumen: ResumenMes,
    val porCategoria: List<TotalCategoria>,
    val ultimosMeses: List<TotalMes>,
    val recientes: List<Gasto>,
    val ingresosMes: Double,
    val indicadores: List<Indicador>,
)

sealed interface InicioUiState {
    data object Cargando : InicioUiState
    data class Listo(val panel: PanelInicio, val actualizando: Boolean, val aviso: String?) : InicioUiState
}

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val sesion: SesionRepository,
    private val gastos: GastosRepository,
    private val categorias: CategoriasRepository,
    private val ingresos: IngresosRepository,
    private val indicadores: IndicadoresRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val actualizando = MutableStateFlow(true)
    private val aviso = MutableStateFlow<String?>(null)
    /** Hasta el primer refresco: con la caché vacía se muestra el esqueleto, no ceros. */
    private val primerRefresco = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val panel: Flow<PanelInicio> = sesion.estado.filterIsInstance<EstadoSesion.Activa>()
        .flatMapLatest { (usuario) ->
            val hoy = hoyEn(usuario.zonaHoraria, reloj)
            val mes = YearMonth.from(hoy)
            val delMes = combine(gastos.gastosDelMes(mes), gastos.gastosDelMes(mes.minusMonths(1)), ::Pair)
            combine(
                delMes,
                gastos.totales(mes, 6),
                gastos.recientes(6),
                ingresos.ingresosDelMes(mes),
                indicadores.indicadores,
            ) { (actual, anterior), meses, recientes, ingresosMes, indicadores ->
                PanelInicio(
                    usuario = usuario,
                    hoy = hoy,
                    resumen = resumenMes(hoy, actual, anterior),
                    porCategoria = totalesPorCategoria(actual).take(6),
                    ultimosMeses = meses,
                    recientes = recientes,
                    ingresosMes = ingresosMes.sumOf { it.monto },
                    indicadores = indicadores,
                )
            }
        }

    val uiState: StateFlow<InicioUiState> =
        combine(panel, actualizando, aviso, primerRefresco) { panel, actualizando, aviso, listo ->
            val vacio = panel.recientes.isEmpty() && panel.ultimosMeses.all { it.total == 0.0 }
            if (!listo && vacio) InicioUiState.Cargando else InicioUiState.Listo(panel, actualizando, aviso)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState.Cargando)

    init {
        actualizar()
    }

    /** Pide al servidor todo lo del Inicio en paralelo; sin conexión, queda lo guardado con un aviso. */
    fun actualizar() {
        val usuario = (sesion.estado.value as? EstadoSesion.Activa)?.usuario ?: return
        val mes = YearMonth.from(hoyEn(usuario.zonaHoraria, reloj))
        actualizando.value = true
        viewModelScope.launch {
            // Los indicadores son de otra API: si fallan no se avisa aquí.
            launch { indicadores.actualizar() }
            val resultados = listOf(
                async { gastos.refrescarMes(mes) },
                async { gastos.refrescarMes(mes.minusMonths(1)) },
                async { gastos.refrescarTotales(mes, 12) },
                async { categorias.refrescar() },
            ).awaitAll()
            val error = resultados.firstNotNullOfOrNull { it.exceptionOrNull() as? ErrorApp }
            aviso.value = error?.takeUnless { it.sesionExpirada }?.let { "${it.mensaje}. Mostrando datos guardados." }
            actualizando.value = false
            primerRefresco.value = true
        }
    }
}
