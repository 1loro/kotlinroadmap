package com.example.controlgastos.data.remote

import com.example.controlgastos.data.local.IndicadorEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.time.Instant
import java.util.concurrent.TimeUnit

// API pública y gratuita con los indicadores del Banco Central de Chile:
// https://mindicador.cl (no requiere clave).
const val INDICADORES_API_URL = "https://mindicador.cl/"

/** Indicadores que muestra la app, en este orden. Todos se expresan en pesos. */
val CODIGOS_INDICADORES = listOf("dolar", "euro", "uf", "utm")

/** Lo que la app necesita de la red. Los tests la reemplazan por una versión falsa. */
interface IndicadoresApi {
    suspend fun obtenerIndicadores(): List<IndicadorDto>
}

// DTO (Data Transfer Object): refleja EXACTAMENTE el JSON de la API.
// Ejemplo de un elemento de la respuesta:
// "dolar": { "codigo": "dolar", "nombre": "Dólar observado", "unidad_medida": "Pesos",
//            "fecha": "2026-10-26T03:00:00.000Z", "valor": 990.35 }
@Serializable
data class IndicadorDto(
    val codigo: String,
    val nombre: String,
    // @SerialName: el JSON usa snake_case y Kotlin camelCase.
    @SerialName("unidad_medida") val unidadMedida: String? = null,
    val fecha: String,
    val valor: Double,
)

fun IndicadorDto.aEntidad(orden: Int) = IndicadorEntity(
    codigo = codigo,
    nombre = nombre,
    valor = valor,
    fecha = Instant.parse(fecha),
    orden = orden,
)

// Lo que Retrofit implementa. La respuesta NO es una lista sino un objeto con
// una clave por indicador ({"version": ..., "dolar": {...}, "uf": {...}}),
// por eso se recibe como JsonObject y se extraen solo los que interesan.
private interface MindicadorServicio {
    @GET("api")
    suspend fun todos(): JsonObject
}

// ignoreUnknownKeys: si la API agrega campos nuevos, la app no falla.
private val json = Json { ignoreUnknownKeys = true }

internal fun JsonObject.extraerIndicadores(): List<IndicadorDto> =
    CODIGOS_INDICADORES.mapNotNull { codigo ->
        (this[codigo] as? JsonObject)?.let { json.decodeFromJsonElement<IndicadorDto>(it) }
    }

fun crearIndicadoresApi(baseUrl: String = INDICADORES_API_URL, registrarPeticiones: Boolean = false): IndicadoresApi {
    val cliente = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .apply {
            // Solo en debug: muestra en Logcat (etiqueta "okhttp") cada petición.
            if (registrarPeticiones) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val servicio: MindicadorServicio = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()

    return object : IndicadoresApi {
        override suspend fun obtenerIndicadores() = servicio.todos().extraerIndicadores()
    }
}
