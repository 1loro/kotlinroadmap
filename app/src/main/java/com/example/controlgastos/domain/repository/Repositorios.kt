package com.example.controlgastos.domain.repository

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.YearMonth

// La capa domain define QUÉ datos necesita la app, con interfaces. La capa
// data decide CÓMO obtenerlos (API de gastos., Room, Keystore). Así los
// ViewModels no dependen de Android y se testean con implementaciones falsas.

/** Error que se puede mostrar tal cual al usuario. */
class ErrorApp(val mensaje: String, val sesionExpirada: Boolean = false) : Exception(mensaje)

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val usuario: Usuario) : EstadoSesion
}

interface SesionRepository {
    val estado: StateFlow<EstadoSesion>
    suspend fun iniciarSesion(email: String, contrasena: String): Result<Usuario>
    suspend fun registrarse(nombre: String, email: String, contrasena: String, zonaHoraria: String): Result<Usuario>
    suspend fun actualizarPerfil(nombre: String, moneda: String, zonaHoraria: String): Result<Usuario>
    /** Vuelve a leer el usuario del servidor (p. ej. si se cambió la foto en la web). */
    suspend fun refrescar(): Result<Usuario>
    /** Foto de perfil (JPEG/PNG/WebP), o null si no tiene. */
    suspend fun avatar(): ByteArray?
    suspend fun cerrarSesion()
}

/**
 * Gastos de la cuenta. Patrón "caché primero": los Flow leen la copia local
 * (funciona sin conexión) y las funciones `refrescar*` la actualizan desde la API.
 */
interface GastosRepository {
    fun gastosDelMes(mes: YearMonth): Flow<List<Gasto>>
    /** Los últimos [cantidad] gastos guardados localmente, de cualquier mes. */
    fun recientes(cantidad: Int): Flow<List<Gasto>>
    fun gasto(id: Long): Flow<Gasto?>
    fun totales(hasta: YearMonth, meses: Int): Flow<List<TotalMes>>

    suspend fun refrescarMes(mes: YearMonth): Result<Unit>
    suspend fun refrescarTotales(hasta: YearMonth, meses: Int): Result<Unit>

    suspend fun crear(monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?): Result<Gasto>
    suspend fun editar(id: Long, monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?): Result<Gasto>
    suspend fun eliminar(id: Long): Result<Unit>
}

interface CategoriasRepository {
    /** Todas, también las archivadas, en el orden de la web. */
    val categorias: Flow<List<Categoria>>
    suspend fun refrescar(): Result<Unit>
    suspend fun crear(nombre: String, icono: String): Result<Categoria>
    suspend fun editar(id: Long, nombre: String, icono: String): Result<Categoria>
    suspend fun archivar(id: Long, archivada: Boolean): Result<Categoria>
    /** @return true si se archivó (tenía gastos) en vez de eliminarse. */
    suspend fun eliminar(id: Long): Result<Boolean>
}

/** Ingresos: solo en este teléfono (el backend de gastos. no los registra). */
interface IngresosRepository {
    fun ingresosDelMes(mes: YearMonth): Flow<List<Ingreso>>
    suspend fun agregar(monto: Double, tipo: TipoIngreso, fecha: LocalDate, nota: String?): Long
    suspend fun eliminar(id: Long)
}

interface IndicadoresRepository {
    /** Últimos indicadores guardados localmente (funciona sin conexión). */
    val indicadores: Flow<List<Indicador>>

    /** Descarga indicadores nuevos. En caso de error, la falla es un [FalloIndicadores]. */
    suspend fun actualizar(): Result<Unit>
}

/** Motivos por los que puede fallar la actualización, con un mensaje para el usuario. */
sealed class FalloIndicadores(val mensaje: String) : Exception(mensaje) {
    class SinConexion : FalloIndicadores("Sin conexión a internet")
    class Servidor(val codigo: Int) : FalloIndicadores("El servidor respondió con error ($codigo)")
    class DatosInvalidos : FalloIndicadores("La respuesta del servidor no es válida")
}

interface PreferenciasRepository {
    val tema: StateFlow<Tema>
    fun elegirTema(tema: Tema)
}
