package com.example.controlgastos.ui.gasto

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.formatearMientrasEscribe
import com.example.controlgastos.domain.hoyEn
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.parsearMonto
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class TipoMovimiento { GASTO, INGRESO }

data class GastoFormUiState(
    val edicion: Boolean,
    val tipo: TipoMovimiento,
    val moneda: String,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val montoTexto: String = "",
    /** Activas, más la archivada que ya use el gasto que se edita (como la web). */
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: Long? = null,
    val tipoIngreso: TipoIngreso? = null,
    val nota: String = "",
    /** "app", "shortcut" o "android" (solo al editar). */
    val origen: String? = null,
    val cargando: Boolean = false,
    val noExiste: Boolean = false,
    val error: String? = null,
    val guardando: Boolean = false,
    /** Se guardó o eliminó: la pantalla debe cerrarse. */
    val terminado: Boolean = false,
)

@HiltViewModel
class GastoFormViewModel @Inject constructor(
    sesion: SesionRepository,
    private val gastos: GastosRepository,
    private val categoriasRepo: CategoriasRepository,
    private val ingresos: IngresosRepository,
    reloj: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Argumentos de las rutas EditarGasto(id) y NuevoGasto(ingreso).
    private val id: Long? = savedStateHandle.get<Long>("id")
    private val usuario = (sesion.estado.value as? EstadoSesion.Activa)?.usuario
    private val hoy = hoyEn(usuario?.zonaHoraria ?: "America/Santiago", reloj)

    private val _uiState = MutableStateFlow(
        GastoFormUiState(
            edicion = id != null,
            tipo = if (savedStateHandle.get<Boolean>("ingreso") == true) TipoMovimiento.INGRESO else TipoMovimiento.GASTO,
            moneda = usuario?.moneda ?: "CLP",
            hoy = hoy,
            fecha = hoy,
            cargando = id != null,
        ),
    )
    val uiState: StateFlow<GastoFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val gasto = id?.let { gastos.gasto(it).first() }
        if (id != null && gasto == null) {
            _uiState.update { it.copy(cargando = false, noExiste = true) }
            return
        }
        if (gasto != null) {
            _uiState.update {
                it.copy(
                    montoTexto = formatearMientrasEscribe(textoMonto(gasto.monto)),
                    categoriaId = gasto.categoriaId,
                    fecha = gasto.fecha,
                    nota = gasto.nota.orEmpty(),
                    origen = gasto.origen,
                    cargando = false,
                )
            }
        }
        // La lista se actualiza sola si cambia (Room); se pide al servidor por si es la primera vez.
        viewModelScope.launch { categoriasRepo.refrescar() }
        categoriasRepo.categorias.collect { todas ->
            val actual = _uiState.value.categoriaId
            _uiState.update { s -> s.copy(categorias = todas.filter { !it.archivada || it.id == actual }) }
        }
    }

    fun cambiarTipo(tipo: TipoMovimiento) = _uiState.update { it.copy(tipo = tipo, error = null) }
    fun cambiarMonto(texto: String) = _uiState.update { it.copy(montoTexto = formatearMientrasEscribe(texto), error = null) }
    fun elegirCategoria(id: Long) = _uiState.update { it.copy(categoriaId = id, error = null) }
    fun elegirTipoIngreso(tipo: TipoIngreso) = _uiState.update { it.copy(tipoIngreso = tipo, error = null) }
    fun elegirFecha(fecha: LocalDate) = _uiState.update { it.copy(fecha = minOf(fecha, hoy), error = null) }
    fun cambiarNota(texto: String) = _uiState.update { it.copy(nota = texto.take(200)) }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.terminado) return
        val monto = parsearMonto(s.montoTexto)
        val nota = s.nota.trim().ifEmpty { null }
        // Mismos mensajes que la web (saveExpense).
        val error = when {
            monto == null -> "Ingresa un monto válido"
            s.tipo == TipoMovimiento.GASTO && s.categoriaId == null -> "Elige una categoría"
            s.tipo == TipoMovimiento.INGRESO && s.tipoIngreso == null -> "Elige un tipo de ingreso"
            else -> null
        }
        if (error != null || monto == null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val resultado: Result<*> = when {
                s.tipo == TipoMovimiento.INGRESO ->
                    Result.success(ingresos.agregar(monto, requireNotNull(s.tipoIngreso), s.fecha, nota))
                id != null -> gastos.editar(id, monto, requireNotNull(s.categoriaId), s.fecha, nota)
                else -> gastos.crear(monto, requireNotNull(s.categoriaId), s.fecha, nota)
            }
            terminar(resultado)
        }
    }

    fun eliminar() {
        val gastoId = id ?: return
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch { terminar(gastos.eliminar(gastoId)) }
    }

    private fun terminar(resultado: Result<*>) {
        _uiState.update {
            it.copy(
                guardando = false,
                terminado = resultado.isSuccess,
                error = resultado.exceptionOrNull()?.let { e -> (e as? ErrorApp)?.mensaje ?: "Error inesperado" },
            )
        }
    }
}

/** 12500.0 -> "12500"; 12.5 -> "12,5" (como String(amount) en la web, con coma decimal). */
private fun textoMonto(monto: Double): String =
    (if (monto % 1.0 == 0.0) monto.toLong().toString() else monto.toString()).replace('.', ',')
