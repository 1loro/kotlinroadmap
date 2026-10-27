package com.example.controlgastos.ui.formulario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.TipoMovimiento
import com.example.controlgastos.domain.usecase.AgregarMovimientoUseCase
import com.example.controlgastos.domain.usecase.ErrorValidacion
import com.example.controlgastos.domain.usecase.NuevoMovimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class FormularioUiState(
    val tipo: TipoMovimiento = TipoMovimiento.GASTO,
    val descripcion: String = "",
    val montoTexto: String = "",
    val categoria: Categoria? = null,
    /** Solo se muestran después del primer intento de guardar. */
    val errores: Set<ErrorValidacion> = emptySet(),
    val guardando: Boolean = false,
    /** true cuando se guardó: la pantalla debe cerrarse. */
    val guardado: Boolean = false,
) {
    val categoriasDisponibles: List<Categoria> get() = Categoria.de(tipo)
}

@HiltViewModel
class FormularioViewModel @Inject constructor(
    private val agregarMovimiento: AgregarMovimientoUseCase,
    private val reloj: Clock,
) : ViewModel() {

    // Patrón: MutableStateFlow PRIVADO (solo el ViewModel lo modifica) y
    // StateFlow PÚBLICO de solo lectura para la UI.
    private val _uiState = MutableStateFlow(FormularioUiState())
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    private var intentoGuardar = false

    fun cambiarTipo(tipo: TipoMovimiento) = actualizar {
        // La categoría elegida puede no corresponder al nuevo tipo.
        copy(tipo = tipo, categoria = categoria?.takeIf { it.tipo == tipo })
    }

    fun cambiarDescripcion(texto: String) = actualizar { copy(descripcion = texto) }

    fun cambiarMonto(texto: String) {
        // Pesos chilenos: solo dígitos y puntos de miles (sin decimales).
        if (texto.all { it.isDigit() || it == '.' }) actualizar { copy(montoTexto = texto) }
    }

    fun elegirCategoria(categoria: Categoria) = actualizar { copy(categoria = categoria) }

    fun guardar() {
        if (_uiState.value.guardando || _uiState.value.guardado) return
        intentoGuardar = true
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val errores = agregarMovimiento(_uiState.value.aNuevoMovimiento())
            _uiState.update { it.copy(errores = errores, guardando = false, guardado = errores.isEmpty()) }
        }
    }

    /** Aplica un cambio y, si ya se intentó guardar, revalida en vivo. */
    private fun actualizar(cambio: FormularioUiState.() -> FormularioUiState) {
        _uiState.update { estado ->
            val nuevo = estado.cambio()
            nuevo.copy(errores = if (intentoGuardar) agregarMovimiento.validar(nuevo.aNuevoMovimiento()) else emptySet())
        }
    }

    private fun FormularioUiState.aNuevoMovimiento() =
        NuevoMovimiento(descripcion, montoTexto, categoria, LocalDate.now(reloj))
}
