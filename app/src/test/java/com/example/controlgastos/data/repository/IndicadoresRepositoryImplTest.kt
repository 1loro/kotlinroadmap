package com.example.controlgastos.data.repository

import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.IndicadorEntity
import com.example.controlgastos.data.remote.crearIndicadoresApi
import com.example.controlgastos.domain.repository.FalloIndicadores
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
class IndicadoresRepositoryImplTest {

    private val servidor = MockWebServer()
    private val dao = FakeIndicadorDao()
    private lateinit var repositorio: IndicadoresRepositoryImpl

    @Before fun iniciar() {
        servidor.start()
        repositorio = IndicadoresRepositoryImpl(crearIndicadoresApi(servidor.url("/").toString()), dao)
    }

    @After fun terminar() = servidor.close()

    @Test fun `respuesta correcta guarda solo los indicadores en pesos, en orden`() = runTest {
        // Forma real de https://mindicador.cl/api (abreviada): un objeto con
        // una clave por indicador, más campos que no son indicadores.
        servidor.enqueue(
            MockResponse.Builder().code(200).body(
                """
                {
                  "version": "1.7.0", "autor": "mindicador.cl", "fecha": "2026-10-27T00:00:00.000Z",
                  "uf": {"codigo": "uf", "nombre": "Unidad de fomento (UF)", "unidad_medida": "Pesos",
                         "fecha": "2026-10-26T03:00:00.000Z", "valor": 41221.08},
                  "dolar": {"codigo": "dolar", "nombre": "Dólar observado", "unidad_medida": "Pesos",
                            "fecha": "2026-10-26T03:00:00.000Z", "valor": 990.35, "campoNuevo": "se ignora"},
                  "tpm": {"codigo": "tpm", "nombre": "Tasa Política Monetaria (TPM)", "unidad_medida": "Porcentaje",
                          "fecha": "2026-10-26T03:00:00.000Z", "valor": 4.5}
                }
                """.trimIndent(),
            ).build(),
        )

        val resultado = repositorio.actualizar()

        assertTrue(resultado.isSuccess)
        assertEquals("/api", servidor.takeRequest().url.encodedPath)
        val guardados = repositorio.indicadores.first()
        // Orden fijo (dólar, euro, UF, UTM); la TPM (un porcentaje) no se guarda.
        assertEquals(listOf("dolar", "uf"), guardados.map { it.codigo })
        assertEquals(990.35, guardados[0].valor, 0.0)
        assertEquals("Dólar observado", guardados[0].nombre)
        assertEquals(Instant.parse("2026-10-26T03:00:00Z"), guardados[1].fecha)
    }

    @Test fun `error HTTP devuelve fallo de servidor y conserva la cache`() = runTest {
        dao.insertarTodas(listOf(entidad("dolar")))
        servidor.enqueue(MockResponse.Builder().code(503).build())

        val fallo = repositorio.actualizar().exceptionOrNull()

        assertTrue(fallo is FalloIndicadores.Servidor)
        assertEquals(503, (fallo as FalloIndicadores.Servidor).codigo)
        assertEquals(listOf("dolar"), repositorio.indicadores.first().map { it.codigo })
    }

    @Test fun `respuesta sin indicadores conocidos es invalida y no borra la cache`() = runTest {
        dao.insertarTodas(listOf(entidad("dolar")))
        servidor.enqueue(MockResponse.Builder().code(200).body("""{"version": "2.0"}""").build())

        assertTrue(repositorio.actualizar().exceptionOrNull() is FalloIndicadores.DatosInvalidos)
        assertEquals(listOf("dolar"), repositorio.indicadores.first().map { it.codigo })
    }

    @Test fun `indicador con formato inesperado devuelve datos invalidos`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200).body("""{"dolar": {"codigo": "dolar", "valor": "no es un número"}}""").build(),
        )

        assertTrue(repositorio.actualizar().exceptionOrNull() is FalloIndicadores.DatosInvalidos)
    }

    @Test fun `servidor caido devuelve sin conexion`() = runTest {
        servidor.close()

        assertTrue(repositorio.actualizar().exceptionOrNull() is FalloIndicadores.SinConexion)
    }

    private fun entidad(codigo: String) = IndicadorEntity(codigo, codigo, 1.0, Instant.EPOCH, 0)
}

/** DAO en memoria: reemplaza a Room en tests de JVM (que no tienen SQLite de Android). */
private class FakeIndicadorDao : IndicadorDao {
    private val tabla = MutableStateFlow<List<IndicadorEntity>>(emptyList())

    override fun observarTodas(): Flow<List<IndicadorEntity>> = tabla

    override suspend fun insertarTodas(indicadores: List<IndicadorEntity>) {
        tabla.value = (tabla.value.filter { e -> indicadores.none { it.codigo == e.codigo } } + indicadores)
            .sortedBy { it.orden }
    }

    override suspend fun borrarTodas() {
        tabla.value = emptyList()
    }

    override suspend fun reemplazarTodas(indicadores: List<IndicadorEntity>) {
        tabla.value = indicadores.sortedBy { it.orden }
    }
}
