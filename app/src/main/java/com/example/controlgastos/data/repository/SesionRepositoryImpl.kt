package com.example.controlgastos.data.repository

import com.example.controlgastos.data.local.CategoriaDao
import com.example.controlgastos.data.local.GastoDao
import com.example.controlgastos.data.local.TotalMesDao
import com.example.controlgastos.data.remote.GastosApi
import com.example.controlgastos.data.remote.LoginBody
import com.example.controlgastos.data.remote.PerfilBody
import com.example.controlgastos.data.remote.RegistroBody
import com.example.controlgastos.data.remote.SesionDto
import com.example.controlgastos.data.remote.aModelo
import com.example.controlgastos.data.remote.llamarApi
import com.example.controlgastos.data.seguridad.AlmacenSesion
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.SesionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryImpl @Inject constructor(
    private val api: GastosApi,
    private val almacen: AlmacenSesion,
    private val gastoDao: GastoDao,
    private val categoriaDao: CategoriaDao,
    private val totalMesDao: TotalMesDao,
) : SesionRepository {

    // El estado se DERIVA de lo guardado: si el interceptor HTTP borra la
    // sesión (respuesta 401), la app vuelve sola al login.
    override val estado: StateFlow<EstadoSesion> = almacen.sesion
        .map { it?.let { s -> EstadoSesion.Activa(s.usuario) } ?: EstadoSesion.SinSesion }
        .stateIn(
            CoroutineScope(SupervisorJob()),
            SharingStarted.Eagerly,
            almacen.sesion.value?.let { EstadoSesion.Activa(it.usuario) } ?: EstadoSesion.SinSesion,
        )

    override suspend fun iniciarSesion(email: String, contrasena: String) =
        llamarApi { api.iniciarSesion(LoginBody(email.trim(), contrasena)) }.map { abrir(it) }

    override suspend fun registrarse(nombre: String, email: String, contrasena: String, zonaHoraria: String) =
        llamarApi { api.registrarse(RegistroBody(nombre.trim(), email.trim(), contrasena, zonaHoraria)) }.map { abrir(it) }

    override suspend fun actualizarPerfil(nombre: String, moneda: String, zonaHoraria: String) =
        llamarApi { api.actualizarPerfil(PerfilBody(nombre.trim(), moneda, zonaHoraria)).aModelo() }
            .onSuccess(almacen::actualizarUsuario)

    override suspend fun refrescar() =
        llamarApi { api.yo().aModelo() }.onSuccess(almacen::actualizarUsuario)

    override suspend fun avatar(): ByteArray? {
        val usuario = almacen.sesion.value?.usuario ?: return null
        val version = usuario.versionAvatar ?: return null
        return llamarApi { api.avatar(usuario.id, version).bytes() }.getOrNull()
    }

    override suspend fun cerrarSesion() {
        almacen.borrar()
        borrarCache()
    }

    private suspend fun abrir(sesion: SesionDto): Usuario {
        // Caché limpia: si antes hubo otra cuenta en este teléfono, no se mezclan.
        borrarCache()
        val usuario = sesion.user.aModelo()
        almacen.guardar(sesion.token, usuario)
        return usuario
    }

    /** Borra la copia local de la cuenta. Los ingresos (locales) se conservan. */
    private suspend fun borrarCache() {
        gastoDao.borrarTodos()
        categoriaDao.borrarTodas()
        totalMesDao.borrarTodos()
    }
}
