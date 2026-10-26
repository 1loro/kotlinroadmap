package com.example.controlgastos.data.repository

import com.example.controlgastos.data.local.CotizacionDao
import com.example.controlgastos.data.local.CotizacionEntity
import com.example.controlgastos.data.remote.crearDolarApi
import com.example.controlgastos.domain.repository.FalloCotizaciones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Prueba el repositorio con Retrofit REAL contra un servidor HTTP falso
 * (MockWebServer) que corre en la propia máquina. Así se prueba también la
 * deserialización del JSON, sin depender de internet.
 */
class CotizacionesRepositoryImplTest {

    private val servidor = MockWebServer()
    private val dao = FakeCotizacionDao()
    private lateinit var repositorio: CotizacionesRepositoryImpl

    @Before fun iniciar() {
        servidor.start()
        repositorio = CotizacionesRepositoryImpl(crearDolarApi(servidor.url("/").toString()), dao)
    }

    @After fun terminar() = servidor.close()

    @Test fun `respuesta correcta se guarda en la cache en orden`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200).body(
                """
                [
                  {"moneda":"USD","casa":"oficial","nombre":"Oficial","compra":1510,"venta":1560,
                   "fechaActualizacion":"2026-10-26T16:00:00.000Z"},
                  {"moneda":"USD","casa":"blue","nombre":"Blue","compra":null,"venta":1575.5,
                   "fechaActualizacion":"2026-10-26T19:00:00.000Z","campoNuevo":"se ignora"}
                ]
                """.trimIndent(),
            ).build(),
        )

        val resultado = repositorio.actualizar()

        assertTrue(resultado.isSuccess)
        assertEquals("/v1/dolares", servidor.takeRequest().url.encodedPath)
        val guardadas = repositorio.cotizaciones.first()
        assertEquals(listOf("oficial", "blue"), guardadas.map { it.casa })
        assertEquals(1560.0, guardadas[0].venta!!, 0.0)
        assertEquals(null, guardadas[1].compra)
        assertEquals(Instant.parse("2026-10-26T19:00:00Z"), guardadas[1].actualizada)
    }

    @Test fun `error HTTP devuelve fallo de servidor y conserva la cache`() = runTest {
        dao.insertarTodas(listOf(entidad("oficial")))
        servidor.enqueue(MockResponse.Builder().code(503).build())

        val fallo = repositorio.actualizar().exceptionOrNull()

        assertTrue(fallo is FalloCotizaciones.Servidor)
        assertEquals(503, (fallo as FalloCotizaciones.Servidor).codigo)
        assertEquals(listOf("oficial"), repositorio.cotizaciones.first().map { it.casa })
    }

    @Test fun `JSON invalido devuelve datos invalidos`() = runTest {
        servidor.enqueue(MockResponse.Builder().code(200).body("""{"no":"es una lista"}""").build())

        assertTrue(repositorio.actualizar().exceptionOrNull() is FalloCotizaciones.DatosInvalidos)
    }

    @Test fun `servidor caido devuelve sin conexion`() = runTest {
        servidor.close()

        assertTrue(repositorio.actualizar().exceptionOrNull() is FalloCotizaciones.SinConexion)
    }

    private fun entidad(casa: String) = CotizacionEntity(casa, casa, 1.0, 2.0, Instant.EPOCH, 0)
}

/** DAO en memoria: reemplaza a Room en tests de JVM (que no tienen SQLite de Android). */
private class FakeCotizacionDao : CotizacionDao {
    private val tabla = MutableStateFlow<List<CotizacionEntity>>(emptyList())

    override fun observarTodas(): Flow<List<CotizacionEntity>> = tabla

    override suspend fun insertarTodas(cotizaciones: List<CotizacionEntity>) {
        tabla.value = (tabla.value.filter { e -> cotizaciones.none { it.casa == e.casa } } + cotizaciones)
            .sortedBy { it.orden }
    }

    override suspend fun borrarTodas() {
        tabla.value = emptyList()
    }

    override suspend fun reemplazarTodas(cotizaciones: List<CotizacionEntity>) {
        tabla.value = cotizaciones.sortedBy { it.orden }
    }
}
