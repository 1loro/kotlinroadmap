package com.example.controlgastos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class AuthUiState(
    val nombre: String = "",
    val email: String = "",
    val contrasena: String = "",
    val error: String? = null,
    val enviando: Boolean = false,
)

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Login y registro. Al tener éxito no navega: la app cambia sola al ver la sesión. */
@HiltViewModel
class AuthViewModel @Inject constructor(private val sesion: SesionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun cambiarNombre(v: String) = _uiState.update { it.copy(nombre = v.take(60), error = null) }
    fun cambiarEmail(v: String) = _uiState.update { it.copy(email = v.trim(), error = null) }
    fun cambiarContrasena(v: String) = _uiState.update { it.copy(contrasena = v, error = null) }

    fun iniciarSesion() = enviar(validar = { null }) { s ->
        sesion.iniciarSesion(s.email.lowercase(), s.contrasena)
    }

    /** Mismas reglas que el registro web (se validan aquí para avisar al instante). */
    fun registrarse() = enviar(
        validar = { s ->
            when {
                s.nombre.isBlank() -> "Ingresa tu nombre"
                !EMAIL.matches(s.email) -> "Email inválido"
                s.contrasena.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
                else -> null
            }
        },
    ) { s -> sesion.registrarse(s.nombre, s.email.lowercase(), s.contrasena, ZoneId.systemDefault().id) }

    private fun enviar(validar: (AuthUiState) -> String?, accion: suspend (AuthUiState) -> Result<*>) {
        val estado = _uiState.value
        if (estado.enviando) return
        validar(estado)?.let { error -> _uiState.update { it.copy(error = error) }; return }
        _uiState.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val resultado = accion(estado)
            _uiState.update {
                it.copy(
                    enviando = false,
                    error = resultado.exceptionOrNull()?.let { e -> (e as? ErrorApp)?.mensaje ?: "Error inesperado" },
                )
            }
        }
    }
}
