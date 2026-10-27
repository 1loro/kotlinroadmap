package com.example.controlgastos.ui.seguridad

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import javax.inject.Inject

/**
 * Decide cuándo la app debe estar bloqueada: al abrirla y al volver después
 * de [TIEMPO_GRACIA_MS] en segundo plano. Vive en un ViewModel para que una
 * rotación (que recrea la Activity) no vuelva a pedir la huella.
 */
@HiltViewModel
class BloqueoViewModel @Inject constructor(private val reloj: Clock) : ViewModel() {

    private val _bloqueada = MutableStateFlow(true)
    val bloqueada: StateFlow<Boolean> = _bloqueada.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var enSegundoPlanoDesde: Long? = null

    fun desbloqueada() {
        _bloqueada.value = false
        _error.value = null
    }

    fun falloAutenticacion(mensaje: String) {
        _error.value = mensaje
    }

    /** El dispositivo no tiene ningún método de bloqueo: no se puede exigir. */
    fun sinMetodoDisponible() = desbloqueada()

    fun pasoASegundoPlano() {
        enSegundoPlanoDesde = reloj.millis()
    }

    fun volvioAPrimerPlano() {
        val desde = enSegundoPlanoDesde ?: return
        enSegundoPlanoDesde = null
        if (reloj.millis() - desde >= TIEMPO_GRACIA_MS) _bloqueada.value = true
    }

    companion object {
        const val TIEMPO_GRACIA_MS = 30_000L
    }
}
