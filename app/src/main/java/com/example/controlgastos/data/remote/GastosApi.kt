package com.example.controlgastos.data.remote

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.ErrorApp
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

// Cliente de la API de gastos. (la misma de la PWA): https://gastos-pi-five.vercel.app
// Las rutas /api/app/... se agregaron al backend para esta app.
const val GASTOS_API_URL = "https://gastos-pi-five.vercel.app/"

interface GastosApi {
    @POST("api/app/auth/login")
    suspend fun iniciarSesion(@Body body: LoginBody): SesionDto

    @POST("api/app/auth/registro")
    suspend fun registrarse(@Body body: RegistroBody): SesionDto

    @GET("api/app/yo")
    suspend fun yo(): UsuarioDto

    @PATCH("api/app/yo")
    suspend fun actualizarPerfil(@Body body: PerfilBody): UsuarioDto

    @GET("api/app/gastos")
    suspend fun gastos(@Query("mes") mes: String): GastosMesDto

    @POST("api/app/gastos")
    suspend fun crearGasto(@Body body: GastoBody): GastoDto

    @PATCH("api/app/gastos/{id}")
    suspend fun editarGasto(@Path("id") id: Long, @Body body: GastoBody): GastoDto

    @DELETE("api/app/gastos/{id}")
    suspend fun eliminarGasto(@Path("id") id: Long): OkDto

    @GET("api/app/categorias")
    suspend fun categorias(): List<CategoriaDto>

    @POST("api/app/categorias")
    suspend fun crearCategoria(@Body body: CategoriaBody): CategoriaDto

    @PATCH("api/app/categorias/{id}")
    suspend fun editarCategoria(@Path("id") id: Long, @Body body: CategoriaBody): CategoriaDto

    @DELETE("api/app/categorias/{id}")
    suspend fun eliminarCategoria(@Path("id") id: Long): OkDto

    @GET("api/app/totales")
    suspend fun totales(@Query("hasta") hasta: String, @Query("meses") meses: Int): TotalesDto

    // Fuera de /api: la foto la sirve la web, con la misma autorización.
    @GET("avatar/{id}")
    suspend fun avatar(@Path("id") id: Long, @Query("v") version: Long): ResponseBody
}

// --- DTO: reflejan exactamente el JSON del backend ---

@Serializable data class LoginBody(val email: String, val password: String)

@Serializable data class RegistroBody(val name: String, val email: String, val password: String, val timezone: String)

@Serializable data class PerfilBody(val name: String, val currency: String, val timezone: String)

@Serializable data class GastoBody(val amount: Double, val categoryId: Long, val spentOn: String, val note: String?)

/** Para PATCH, los campos null no se envían (explicitNulls = false). */
@Serializable data class CategoriaBody(val name: String? = null, val icon: String? = null, val archived: Boolean? = null)

@Serializable data class SesionDto(val token: String, val user: UsuarioDto)

@Serializable
data class UsuarioDto(
    val id: Long,
    val name: String,
    val email: String,
    val currency: String,
    val timezone: String,
    val avatarVersion: Long? = null,
)

@Serializable
data class GastoDto(
    val id: Long,
    val amount: Double,
    val note: String? = null,
    val spentOn: String,
    val source: String = "app",
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
)

@Serializable data class GastosMesDto(val month: String, val expenses: List<GastoDto>)

@Serializable
data class CategoriaDto(
    val id: Long,
    val name: String,
    val icon: String,
    val sort: Int = 0,
    val archived: Boolean = false,
    val uses: Int = 0,
)

@Serializable data class TotalMesDto(val month: String, val total: Double)

@Serializable data class TotalesDto(val today: String, val months: List<TotalMesDto>)

@Serializable data class OkDto(val ok: Boolean = true, val archived: Boolean = false)

// --- Conversiones a modelos de la app ---

fun UsuarioDto.aModelo() = Usuario(id, name, email, currency, timezone, avatarVersion)

fun Usuario.aDto() = UsuarioDto(id, nombre, email, moneda, zonaHoraria, versionAvatar)

fun GastoDto.aModelo() =
    Gasto(id, amount, note, LocalDate.parse(spentOn), source, categoryId, categoryName, categoryIcon)

fun CategoriaDto.aModelo() = Categoria(id, name, icon, sort, archived, uses)

fun TotalMesDto.aModelo() = TotalMes(YearMonth.parse(month), total)

// --- Construcción del cliente ---

val jsonApi = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * @param token sesión actual (o null): se envía como "Authorization: Bearer".
 * @param sesionExpirada se llama si el servidor responde 401 a una petición con token.
 */
fun crearGastosApi(
    baseUrl: String,
    token: () -> String?,
    sesionExpirada: () -> Unit,
    registrarPeticiones: Boolean = false,
): GastosApi {
    val autorizacion = Interceptor { chain ->
        val actual = token()
        val peticion = chain.request().newBuilder().apply {
            if (actual != null) header("Authorization", "Bearer $actual")
        }.build()
        val respuesta = chain.proceed(peticion)
        if (respuesta.code == 401 && actual != null) sesionExpirada()
        respuesta
    }
    val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(autorizacion)
        .apply {
            // BASIC: método, URL y código. Nunca BODY: mostraría contraseñas y tokens.
            if (registrarPeticiones) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(jsonApi.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
}

/**
 * Ejecuta una llamada a la API y traduce cualquier falla a un [ErrorApp] con un
 * mensaje para el usuario (el backend responde {"error": "..."}).
 * No usa runCatching para no atrapar la cancelación de corrutinas.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: HttpException) {
    val mensaje = e.response()?.errorBody()?.string()?.let { cuerpo ->
        try {
            jsonApi.parseToJsonElement(cuerpo).jsonObject["error"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    } ?: "El servidor respondió con error (${e.code()})"
    Result.failure(ErrorApp(mensaje, sesionExpirada = e.code() == 401))
} catch (e: IOException) {
    Result.failure(ErrorApp("Sin conexión a internet"))
} catch (e: SerializationException) {
    Result.failure(ErrorApp("La respuesta del servidor no es válida"))
} catch (e: DateTimeException) {
    // Fechas o meses con formato inesperado (LocalDate.parse, YearMonth.parse).
    Result.failure(ErrorApp("La respuesta del servidor no es válida"))
}
