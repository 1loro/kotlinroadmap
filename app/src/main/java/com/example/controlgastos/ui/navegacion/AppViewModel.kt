package com.example.controlgastos.ui.navegacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.PreferenciasRepository
import com.example.controlgastos.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de toda la app: sesión (login o app), tema y foto de perfil. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sesion: SesionRepository,
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = sesion.estado
    val tema: StateFlow<Tema> = preferencias.tema

    /** Foto de perfil del usuario actual; se vuelve a pedir si cambia su versión. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val avatar: StateFlow<ByteArray?> = sesion.estado
        .map { (it as? EstadoSesion.Activa)?.usuario?.let { u -> u.id to u.versionAvatar } }
        .distinctUntilChanged()
        .mapLatest { clave -> if (clave?.second == null) null else sesion.avatar() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** El botón de la barra superior alterna claro/oscuro (como ThemeToggle en la web). */
    fun alternarTema(oscuroAhora: Boolean) = preferencias.elegirTema(if (oscuroAhora) Tema.CLARO else Tema.OSCURO)

    init {
        // Al abrir la app con sesión guardada, se actualizan nombre/foto/moneda
        // por si se cambiaron desde el iPhone. Sin conexión, se usa lo guardado.
        if (sesion.estado.value is EstadoSesion.Activa) viewModelScope.launch { sesion.refrescar() }
    }
}
