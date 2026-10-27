package com.example.controlgastos.ui.indicadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.FalloIndicadores
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Lo que en la Fase 3 hacía IndicadoresRuta (estado + LaunchedEffect) ahora
// vive aquí: sobrevive a la rotación y se testea sin Compose.
@HiltViewModel
class IndicadoresViewModel @Inject constructor(
    private val repositorio: IndicadoresRepository,
) : ViewModel() {

    private val refresco = MutableStateFlow<Refresco>(Refresco.EnCurso)
    private var trabajo: Job? = null

    val uiState: StateFlow<IndicadoresUiState> = combine(
        // null = Room todavía no respondió.
        repositorio.indicadores.map<List<Indicador>, List<Indicador>?> { it }.onStart { emit(null) },
        refresco,
        ::calcularEstado,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndicadoresUiState.Cargando)

    init {
        actualizar()
    }

    fun actualizar() {
        // Si ya hay una petición en curso, no se lanza otra.
        if (trabajo?.isActive == true) return
        trabajo = viewModelScope.launch {
            refresco.value = Refresco.EnCurso
            refresco.value = repositorio.actualizar().fold(
                onSuccess = { Refresco.Completado },
                onFailure = { Refresco.Fallido((it as? FalloIndicadores)?.mensaje ?: "Error inesperado") },
            )
        }
    }
}
