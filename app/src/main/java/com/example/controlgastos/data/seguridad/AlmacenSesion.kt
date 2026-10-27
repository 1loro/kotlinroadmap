package com.example.controlgastos.data.seguridad

import android.content.Context
import androidx.core.content.edit
import com.example.controlgastos.data.remote.UsuarioDto
import com.example.controlgastos.data.remote.aDto
import com.example.controlgastos.data.remote.aModelo
import com.example.controlgastos.data.remote.jsonApi
import com.example.controlgastos.domain.model.Usuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda la sesión (token + datos del usuario) CIFRADA con el Keystore: el
 * token da acceso a la cuenta, así que nunca se escribe en texto plano.
 * Se lee al iniciar la app para no pedir login cada vez.
 */
@Singleton
class AlmacenSesion @Inject constructor(
    @ApplicationContext context: Context,
    private val claves: GestorClaves,
) {
    data class Sesion(val token: String, val usuario: Usuario)

    @Serializable
    private data class Guardada(val token: String, val user: UsuarioDto)

    private val preferencias = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    private val _sesion = MutableStateFlow(leer())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    /** Token actual para el header Authorization (lo lee el interceptor HTTP). */
    val token: String? get() = _sesion.value?.token

    fun guardar(token: String, usuario: Usuario) = escribir(Sesion(token, usuario))

    fun actualizarUsuario(usuario: Usuario) {
        _sesion.value?.let { escribir(it.copy(usuario = usuario)) }
    }

    fun borrar() {
        preferencias.edit(commit = true) { remove(CLAVE) }
        _sesion.value = null
    }

    private fun escribir(sesion: Sesion) {
        val texto = jsonApi.encodeToString(Guardada.serializer(), Guardada(sesion.token, sesion.usuario.aDto()))
        preferencias.edit(commit = true) { putString(CLAVE, claves.cifrar(texto.toByteArray())) }
        _sesion.value = sesion
    }

    private fun leer(): Sesion? = try {
        preferencias.getString(CLAVE, null)?.let { cifrado ->
            val guardada = jsonApi.decodeFromString(Guardada.serializer(), claves.descifrar(cifrado).decodeToString())
            Sesion(guardada.token, guardada.user.aModelo())
        }
    } catch (e: Exception) {
        // Datos ilegibles (p. ej. la clave del Keystore se regeneró): se pide login de nuevo.
        null
    }

    private companion object {
        const val ARCHIVO = "sesion_cifrada"
        const val CLAVE = "sesion"
    }
}
