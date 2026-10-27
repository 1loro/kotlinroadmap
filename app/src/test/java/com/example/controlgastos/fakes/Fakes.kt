package com.example.controlgastos.fakes

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.PreferenciasRepository
import com.example.controlgastos.domain.repository.SesionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

// Implementaciones FALSAS de las interfaces del dominio, en memoria. Gracias
// a que los ViewModels dependen de interfaces (no de Retrofit ni Room), los
// tests corren en la JVM, en milisegundos, sin emulador ni servidor.

val USUARIO = Usuario(1, "Lorenzo Teixido", "lorenzo@ejemplo.cl", "CLP", "America/Santiago", null)

class FakeSesionRepository(usuario: Usuario? = USUARIO) : SesionRepository {
    private val _estado = MutableStateFlow<EstadoSesion>(usuario?.let { EstadoSesion.Activa(it) } ?: EstadoSesion.SinSesion)
    override val estado: StateFlow<EstadoSesion> = _estado

    /** Si no es null, las operaciones fallan con este error. */
    var error: ErrorApp? = null
    var ultimoLogin: Pair<String, String>? = null
        private set

    private fun resultado(usuario: Usuario): Result<Usuario> {
        error?.let { return Result.failure(it) }
        _estado.value = EstadoSesion.Activa(usuario)
        return Result.success(usuario)
    }

    override suspend fun iniciarSesion(email: String, contrasena: String): Result<Usuario> {
        ultimoLogin = email to contrasena
        return resultado(USUARIO.copy(email = email))
    }

    override suspend fun registrarse(nombre: String, email: String, contrasena: String, zonaHoraria: String) =
        resultado(USUARIO.copy(nombre = nombre, email = email, zonaHoraria = zonaHoraria))

    override suspend fun actualizarPerfil(nombre: String, moneda: String, zonaHoraria: String) =
        resultado((estado.value as EstadoSesion.Activa).usuario.copy(nombre = nombre, moneda = moneda, zonaHoraria = zonaHoraria))

    override suspend fun refrescar() = resultado((estado.value as EstadoSesion.Activa).usuario)
    override suspend fun avatar(): ByteArray? = null
    override suspend fun cerrarSesion() {
        _estado.value = EstadoSesion.SinSesion
    }
}

class FakeGastosRepository(iniciales: List<Gasto> = emptyList()) : GastosRepository {
    private val tabla = MutableStateFlow(iniciales)
    val guardados: List<Gasto> get() = tabla.value
    var totales: Map<YearMonth, Double> = emptyMap()

    /** Si no es null, refrescar/crear/editar/eliminar fallan con este error. */
    var error: ErrorApp? = null
    val mesesRefrescados = mutableListOf<YearMonth>()

    private fun ordenados(lista: List<Gasto>) =
        lista.sortedWith(compareByDescending<Gasto> { it.fecha }.thenByDescending { it.id })

    override fun gastosDelMes(mes: YearMonth): Flow<List<Gasto>> = tabla.map { l -> ordenados(l.filter { it.mes == mes }) }
    override fun recientes(cantidad: Int): Flow<List<Gasto>> = tabla.map { ordenados(it).take(cantidad) }
    override fun gasto(id: Long): Flow<Gasto?> = tabla.map { l -> l.find { it.id == id } }
    override fun totales(hasta: YearMonth, meses: Int): Flow<List<TotalMes>> = tabla.map {
        (0 until meses).map { i ->
            val mes = hasta.minusMonths((meses - 1 - i).toLong())
            TotalMes(mes, totales[mes] ?: 0.0)
        }
    }

    private fun <T> falla(): Result<T>? = error?.let { Result.failure(it) }

    override suspend fun refrescarMes(mes: YearMonth): Result<Unit> {
        mesesRefrescados += mes
        return falla() ?: Result.success(Unit)
    }

    override suspend fun refrescarTotales(hasta: YearMonth, meses: Int) = falla() ?: Result.success(Unit)

    override suspend fun crear(monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?): Result<Gasto> {
        falla<Gasto>()?.let { return it }
        val nuevo = Gasto((tabla.value.maxOfOrNull { it.id } ?: 0) + 1, monto, nota, fecha, "android", categoriaId, "Cat $categoriaId", "receipt")
        tabla.update { it + nuevo }
        return Result.success(nuevo)
    }

    override suspend fun editar(id: Long, monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?): Result<Gasto> {
        falla<Gasto>()?.let { return it }
        val editado = tabla.value.first { it.id == id }.copy(monto = monto, categoriaId = categoriaId, fecha = fecha, nota = nota)
        tabla.update { l -> l.map { if (it.id == id) editado else it } }
        return Result.success(editado)
    }

    override suspend fun eliminar(id: Long): Result<Unit> {
        falla<Unit>()?.let { return it }
        tabla.update { l -> l.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}

class FakeCategoriasRepository(iniciales: List<Categoria> = CATEGORIAS) : CategoriasRepository {
    private val tabla = MutableStateFlow(iniciales)
    override val categorias: Flow<List<Categoria>> = tabla
    var error: ErrorApp? = null

    override suspend fun refrescar() = error?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)

    override suspend fun crear(nombre: String, icono: String): Result<Categoria> {
        error?.let { return Result.failure(it) }
        val nueva = Categoria((tabla.value.maxOfOrNull { it.id } ?: 0) + 1, nombre, icono)
        tabla.update { it + nueva }
        return Result.success(nueva)
    }

    override suspend fun editar(id: Long, nombre: String, icono: String) = cambiar(id) { it.copy(nombre = nombre, icono = icono) }
    override suspend fun archivar(id: Long, archivada: Boolean) = cambiar(id) { it.copy(archivada = archivada) }

    override suspend fun eliminar(id: Long): Result<Boolean> {
        val cat = tabla.value.first { it.id == id }
        return if (cat.usos > 0) {
            cambiar(id) { it.copy(archivada = true) }
            Result.success(true)
        } else {
            tabla.update { l -> l.filterNot { it.id == id } }
            Result.success(false)
        }
    }

    private fun cambiar(id: Long, f: (Categoria) -> Categoria): Result<Categoria> {
        error?.let { return Result.failure(it) }
        tabla.update { l -> l.map { if (it.id == id) f(it) else it } }
        return Result.success(tabla.value.first { it.id == id })
    }

    companion object {
        val CATEGORIAS = listOf(
            Categoria(1, "Comida", "utensils", 0, usos = 3),
            Categoria(2, "Hogar", "house", 1),
            Categoria(3, "Viejita", "receipt", 2, archivada = true, usos = 1),
        )
    }
}

class FakeIngresosRepository : IngresosRepository {
    private val tabla = MutableStateFlow<List<Ingreso>>(emptyList())
    val guardados: List<Ingreso> get() = tabla.value

    override fun ingresosDelMes(mes: YearMonth): Flow<List<Ingreso>> = tabla.map { l -> l.filter { YearMonth.from(it.fecha) == mes } }

    override suspend fun agregar(monto: Double, tipo: TipoIngreso, fecha: LocalDate, nota: String?): Long {
        val id = (tabla.value.maxOfOrNull { it.id } ?: 0) + 1
        tabla.update { it + Ingreso(id, monto, tipo, nota, fecha) }
        return id
    }

    override suspend fun eliminar(id: Long) = tabla.update { l -> l.filterNot { it.id == id } }
}

class FakePreferenciasRepository : PreferenciasRepository {
    private val _tema = MutableStateFlow(Tema.SISTEMA)
    override val tema: StateFlow<Tema> = _tema
    override fun elegirTema(tema: Tema) {
        _tema.value = tema
    }
}

class FakeIndicadoresRepository(
    iniciales: List<Indicador> = emptyList(),
    /** Lo que devolverá la próxima llamada a actualizar(). */
    var resultado: Result<Unit> = Result.success(Unit),
    /** Indicadores que "llegan de la red" si la actualización tiene éxito. */
    var desdeLaRed: List<Indicador> = iniciales,
) : IndicadoresRepository {
    /** Si no es null, actualizar() espera a que se complete: simula una red lenta. */
    var pausa: CompletableDeferred<Unit>? = null

    private val cache = MutableStateFlow(iniciales)
    var llamadasActualizar = 0
        private set

    override val indicadores: Flow<List<Indicador>> = cache

    override suspend fun actualizar(): Result<Unit> {
        llamadasActualizar++
        pausa?.await()
        if (resultado.isSuccess) cache.value = desdeLaRed
        return resultado
    }
}
