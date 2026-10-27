package com.example.controlgastos.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.PreferenciasRepository
import com.example.controlgastos.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mismas opciones que src/components/preferences-form.tsx. */
val MONEDAS = listOf(
    "CLP" to "Peso chileno",
    "ARS" to "Peso argentino",
    "MXN" to "Peso mexicano",
    "COP" to "Peso colombiano",
    "PEN" to "Sol peruano",
    "UYU" to "Peso uruguayo",
    "USD" to "Dólar estadounidense",
    "EUR" to "Euro",
)

val ZONAS = listOf(
    "America/Santiago",
    "America/Argentina/Buenos_Aires",
    "America/Mexico_City",
    "America/Bogota",
    "America/Lima",
    "America/Montevideo",
    "America/New_York",
    "Europe/Madrid",
)

data class FormPerfil(
    val nombre: String = "",
    val moneda: String = "CLP",
    val zona: String = "America/Santiago",
    val guardando: Boolean = false,
    /** "Guardado" o el error del servidor. */
    val mensaje: String? = null,
    val esError: Boolean = false,
)

/** Editor de categoría: `id == null` es una nueva. */
data class EditorCategoria(val id: Long?, val nombre: String, val icono: String, val error: String? = null, val guardando: Boolean = false)

data class AjustesUiState(
    val usuario: Usuario? = null,
    val tema: Tema = Tema.SISTEMA,
    val perfil: FormPerfil = FormPerfil(),
    val categorias: List<Categoria> = emptyList(),
    val editor: EditorCategoria? = null,
    val avisoCategorias: String? = null,
)

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val sesion: SesionRepository,
    private val categoriasRepo: CategoriasRepository,
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    private val perfil = MutableStateFlow(
        (sesion.estado.value as? EstadoSesion.Activa)?.usuario?.let { FormPerfil(it.nombre, it.moneda, it.zonaHoraria) } ?: FormPerfil(),
    )
    private val editor = MutableStateFlow<EditorCategoria?>(null)
    private val avisoCategorias = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AjustesUiState> = combine(
        combine(sesion.estado, preferencias.tema, ::Pair),
        perfil,
        categoriasRepo.categorias,
        editor,
        avisoCategorias,
    ) { (estado, tema), perfil, categorias, editor, aviso ->
        AjustesUiState((estado as? EstadoSesion.Activa)?.usuario, tema, perfil, categorias, editor, aviso)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState())

    init {
        viewModelScope.launch {
            categoriasRepo.refrescar().onFailure { e -> avisoCategorias.value = (e as? ErrorApp)?.mensaje }
        }
    }

    // --- Perfil ---
    fun cambiarNombre(v: String) = perfil.update { it.copy(nombre = v.take(60), mensaje = null) }
    fun cambiarMoneda(v: String) = perfil.update { it.copy(moneda = v, mensaje = null) }
    fun cambiarZona(v: String) = perfil.update { it.copy(zona = v, mensaje = null) }

    fun guardarPerfil() {
        val p = perfil.value
        if (p.guardando) return
        if (p.nombre.isBlank()) {
            perfil.update { it.copy(mensaje = "Ingresa tu nombre", esError = true) }
            return
        }
        perfil.update { it.copy(guardando = true, mensaje = null) }
        viewModelScope.launch {
            val resultado = sesion.actualizarPerfil(p.nombre, p.moneda, p.zona)
            perfil.update {
                it.copy(
                    guardando = false,
                    mensaje = resultado.exceptionOrNull()?.let { e -> (e as? ErrorApp)?.mensaje ?: "Error inesperado" } ?: "Guardado",
                    esError = resultado.isFailure,
                )
            }
        }
    }

    fun elegirTema(tema: Tema) = preferencias.elegirTema(tema)

    // --- Categorías ---
    fun nuevaCategoria() { editor.value = EditorCategoria(null, "", "receipt") }
    fun editarCategoria(c: Categoria) { editor.value = EditorCategoria(c.id, c.nombre, c.icono) }
    fun cancelarEdicion() { editor.value = null }
    fun cambiarNombreCategoria(v: String) = editor.update { it?.copy(nombre = v.take(40), error = null) }
    fun elegirIcono(clave: String) = editor.update { it?.copy(icono = clave) }

    fun guardarCategoria() {
        val e = editor.value ?: return
        if (e.guardando) return
        if (e.nombre.isBlank()) {
            editor.update { it?.copy(error = "Ponle un nombre") }
            return
        }
        editor.update { it?.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val resultado = if (e.id == null) categoriasRepo.crear(e.nombre, e.icono) else categoriasRepo.editar(e.id, e.nombre, e.icono)
            resultado
                .onSuccess { editor.value = null }
                .onFailure { err -> editor.update { it?.copy(guardando = false, error = (err as? ErrorApp)?.mensaje ?: "Error inesperado") } }
        }
    }

    fun alternarArchivada(c: Categoria) = accionCategoria { categoriasRepo.archivar(c.id, !c.archivada) }

    fun eliminarCategoria(c: Categoria) = accionCategoria { categoriasRepo.eliminar(c.id) }

    private fun accionCategoria(accion: suspend () -> Result<*>) {
        viewModelScope.launch {
            avisoCategorias.value = accion().exceptionOrNull()?.let { (it as? ErrorApp)?.mensaje ?: "Error inesperado" }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { sesion.cerrarSesion() }
    }
}
