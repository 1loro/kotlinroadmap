package com.example.controlgastos.data.remote

import com.example.controlgastos.data.local.CotizacionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.time.Instant
import java.util.concurrent.TimeUnit

// API pública y gratuita: https://dolarapi.com (no requiere clave).
const val DOLAR_API_URL = "https://dolarapi.com/"

// Retrofit genera la implementación de esta interfaz. `suspend` hace que la
// llamada se ejecute sin bloquear el hilo principal.
interface DolarApi {
    @GET("v1/dolares")
    suspend fun obtenerDolares(): List<CotizacionDto>
}

// DTO (Data Transfer Object): refleja EXACTAMENTE el JSON de la API.
// Ejemplo de un elemento de la respuesta:
// { "moneda": "USD", "casa": "oficial", "nombre": "Oficial",
//   "compra": 1510, "venta": 1560, "fechaActualizacion": "2026-10-26T16:00:00.000Z" }
@Serializable
data class CotizacionDto(
    val casa: String,
    val nombre: String,
    val compra: Double? = null,
    val venta: Double? = null,
    val fechaActualizacion: String,
)

fun CotizacionDto.aEntidad(orden: Int) = CotizacionEntity(
    casa = casa,
    nombre = nombre,
    compra = compra,
    venta = venta,
    actualizada = Instant.parse(fechaActualizacion),
    orden = orden,
)

// ignoreUnknownKeys: si la API agrega campos nuevos (como "moneda", que no
// usamos), la app no falla.
private val json = Json { ignoreUnknownKeys = true }

fun crearDolarApi(baseUrl: String = DOLAR_API_URL, registrarPeticiones: Boolean = false): DolarApi {
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

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
}
