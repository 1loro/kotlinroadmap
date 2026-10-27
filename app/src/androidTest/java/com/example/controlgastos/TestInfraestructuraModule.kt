package com.example.controlgastos

import android.content.Context
import com.example.controlgastos.data.di.InfraestructuraModule
import com.example.controlgastos.data.local.CategoriaDao
import com.example.controlgastos.data.local.GastoDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.IngresoDao
import com.example.controlgastos.data.local.TotalMesDao
import com.example.controlgastos.data.remote.CategoriaBody
import com.example.controlgastos.data.remote.CategoriaDto
import com.example.controlgastos.data.remote.GastoBody
import com.example.controlgastos.data.remote.GastoDto
import com.example.controlgastos.data.remote.GastosApi
import com.example.controlgastos.data.remote.GastosMesDto
import com.example.controlgastos.data.remote.IndicadorDto
import com.example.controlgastos.data.remote.IndicadoresApi
import com.example.controlgastos.data.remote.LoginBody
import com.example.controlgastos.data.remote.OkDto
import com.example.controlgastos.data.remote.PerfilBody
import com.example.controlgastos.data.remote.RegistroBody
import com.example.controlgastos.data.remote.SesionDto
import com.example.controlgastos.data.remote.TotalMesDto
import com.example.controlgastos.data.remote.TotalesDto
import com.example.controlgastos.data.remote.UsuarioDto
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** API de indicadores falsa: devuelve lo que diga `respuesta` (o lanza su excepción). */
@Singleton
class FakeIndicadoresApi @Inject constructor() : IndicadoresApi {
    @Volatile var respuesta: () -> List<IndicadorDto> = { emptyList() }
    override suspend fun obtenerIndicadores() = respuesta()
}

/**
 * Backend de gastos. falso, en memoria: misma API que /api/app/..., con una
 * cuenta de prueba. Así los tests de UI recorren la app completa sin tocar
 * el servidor real ni la cuenta de nadie.
 */
@Singleton
class FakeGastosApi @Inject constructor() : GastosApi {
    companion object {
        const val EMAIL = "prueba@gastos.cl"
        const val CONTRASENA = "secreta123"
        val USUARIO = UsuarioDto(1, "Ana Prueba", EMAIL, "CLP", "America/Santiago", null)
    }

    /** true: todas las llamadas fallan como si no hubiera red. */
    @Volatile var sinConexion = false

    private var usuario = USUARIO
    private val categorias = mutableListOf(
        CategoriaDto(1, "Comida", "utensils", 0),
        CategoriaDto(2, "Supermercado", "cart", 1),
        CategoriaDto(3, "Hogar", "house", 2),
        CategoriaDto(4, "Otros", "receipt", 3),
    )
    val gastos = mutableListOf<GastoDto>()

    private fun red() {
        if (sinConexion) throw IOException("sin red")
    }

    private fun error(codigo: Int, mensaje: String): Nothing =
        throw HttpException(Response.error<Any>(codigo, """{"error":"$mensaje"}""".toResponseBody()))

    private fun sesion(): SesionDto = SesionDto("1.9999999999999.firma", usuario)

    override suspend fun iniciarSesion(body: LoginBody): SesionDto {
        red()
        if (body.email != EMAIL || body.password != CONTRASENA) error(401, "Email o contraseña incorrectos")
        return sesion()
    }

    override suspend fun registrarse(body: RegistroBody): SesionDto {
        red()
        usuario = usuario.copy(name = body.name, email = body.email)
        return sesion()
    }

    override suspend fun yo(): UsuarioDto {
        red()
        return usuario
    }

    override suspend fun actualizarPerfil(body: PerfilBody): UsuarioDto {
        red()
        usuario = usuario.copy(name = body.name, currency = body.currency, timezone = body.timezone)
        return usuario
    }

    override suspend fun gastos(mes: String): GastosMesDto {
        red()
        return GastosMesDto(mes, gastos.filter { it.spentOn.startsWith(mes) }.sortedByDescending { it.spentOn })
    }

    private fun armar(id: Long, body: GastoBody, origen: String): GastoDto {
        val cat = categorias.firstOrNull { it.id == body.categoryId } ?: error(400, "Categoría inválida")
        return GastoDto(id, body.amount, body.note, body.spentOn, origen, cat.id, cat.name, cat.icon)
    }

    override suspend fun crearGasto(body: GastoBody): GastoDto {
        red()
        return armar((gastos.maxOfOrNull { it.id } ?: 0) + 1, body, "android").also { gastos += it }
    }

    override suspend fun editarGasto(id: Long, body: GastoBody): GastoDto {
        red()
        val i = gastos.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: error(404, "Gasto no encontrado")
        return armar(id, body, gastos[i].source).also { gastos[i] = it }
    }

    override suspend fun eliminarGasto(id: Long): OkDto {
        red()
        if (!gastos.removeAll { it.id == id }) error(404, "Gasto no encontrado")
        return OkDto()
    }

    override suspend fun categorias(): List<CategoriaDto> {
        red()
        return categorias.map { c -> c.copy(uses = gastos.count { it.categoryId == c.id }) }
    }

    override suspend fun crearCategoria(body: CategoriaBody): CategoriaDto {
        red()
        return CategoriaDto((categorias.maxOf { it.id }) + 1, body.name.orEmpty(), body.icon ?: "receipt", categorias.size)
            .also { categorias += it }
    }

    override suspend fun editarCategoria(id: Long, body: CategoriaBody): CategoriaDto {
        red()
        val i = categorias.indexOfFirst { it.id == id }
        categorias[i] = categorias[i].let { it.copy(name = body.name ?: it.name, icon = body.icon ?: it.icon, archived = body.archived ?: it.archived) }
        return categorias[i]
    }

    override suspend fun eliminarCategoria(id: Long): OkDto {
        red()
        categorias.removeAll { it.id == id }
        return OkDto()
    }

    override suspend fun totales(hasta: String, meses: Int): TotalesDto {
        red()
        val fin = java.time.YearMonth.parse(hasta)
        val porMes = (0 until meses).map { i ->
            val mes = fin.minusMonths((meses - 1 - i).toLong()).toString()
            TotalMesDto(mes, gastos.filter { it.spentOn.startsWith(mes) }.sumOf { it.amount })
        }
        return TotalesDto(LocalDate.now().toString(), porMes)
    }

    override suspend fun avatar(id: Long, version: Long): ResponseBody = error(404, "Sin foto")
}

/**
 * En TODOS los tests instrumentados, Hilt usa este módulo en lugar de
 * InfraestructuraModule: BD en memoria (nueva en cada test) y APIs falsas
 * (sin red). El resto de la app (repositorios, ViewModels, pantallas) es el
 * código REAL.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [InfraestructuraModule::class])
object TestInfraestructuraModule {

    @Provides @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): GastosDatabase = GastosDatabase.construirEnMemoria(context)

    @Provides fun gastoDao(db: GastosDatabase): GastoDao = db.gastoDao()
    @Provides fun categoriaDao(db: GastosDatabase): CategoriaDao = db.categoriaDao()
    @Provides fun totalMesDao(db: GastosDatabase): TotalMesDao = db.totalMesDao()
    @Provides fun ingresoDao(db: GastosDatabase): IngresoDao = db.ingresoDao()
    @Provides fun indicadorDao(db: GastosDatabase): IndicadorDao = db.indicadorDao()

    @Provides fun gastosApi(fake: FakeGastosApi): GastosApi = fake
    @Provides fun indicadoresApi(fake: FakeIndicadoresApi): IndicadoresApi = fake
}
