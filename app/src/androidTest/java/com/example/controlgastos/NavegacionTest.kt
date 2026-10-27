package com.example.controlgastos

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ActivityScenario
import com.example.controlgastos.data.remote.IndicadorDto
import com.example.controlgastos.ui.formatearDolares
import com.example.controlgastos.ui.formatearMonto
import com.example.controlgastos.ui.formatearValorIndicador
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.io.IOException
import javax.inject.Inject

// Prueba de UI de la app COMPLETA (MainActivity + Hilt + ViewModels + Room
// en memoria), con la red reemplazada por FakeIndicadoresApi.
// ./gradlew :app:connectedDebugAndroidTest
@HiltAndroidTest
class NavegacionTest {

    private val hilt = HiltAndroidRule(this)
    // "Empty": la regla no lanza ninguna Activity; cada test la lanza cuando
    // ya configuró la API falsa (si no, la app pediría indicadores antes).
    private val compose = createEmptyComposeRule()

    // Hilt primero (crea el grafo de dependencias), luego Compose.
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(hilt).around(compose)

    @Inject lateinit var api: FakeIndicadoresApi
    @Inject lateinit var autenticador: FakeAutenticador

    private var escenario: ActivityScenario<MainActivity>? = null

    @Before fun inyectar() = hilt.inject()

    @After fun cerrar() {
        escenario?.close()
    }

    private fun abrirApp() {
        escenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test fun agregarVerYEliminarUnGasto() {
        abrirApp()
        compose.esperarTexto("Feria")

        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithText("Guardar").performClick()
        compose.esperarTexto("Escribe una descripción")

        compose.onNodeWithText("Descripción").performTextInput("Café de prueba")
        compose.onNodeWithText("Monto").performTextInput("1.234")
        compose.onNodeWithText("🎬 Ocio").performClick()
        compose.onNodeWithText("Guardar").performClick()

        compose.esperarTexto("Café de prueba")
        // Pesos chilenos: sin decimales y con punto de miles.
        compose.onNodeWithText("-${formatearMonto(1234.0)}").assertIsDisplayed()
        compose.onNodeWithText("Café de prueba").performClick()

        compose.esperarTexto("Detalle")
        compose.onNodeWithContentDescription("Eliminar").performClick()
        compose.onNodeWithText("Eliminar").performClick()

        compose.esperarHasta("que desaparezca el gasto") { !hayTexto("Café de prueba") }
        compose.onNodeWithText("Control de Gastos").assertIsDisplayed()
    }

    @Test fun filtrarMovimientosPorTipo() {
        abrirApp()
        compose.onNodeWithText("Movimientos").performClick()
        compose.esperarTexto("Arriendo")

        compose.onNodeWithText("Ingresos").performClick()
        compose.esperarHasta("que se oculte el arriendo") { !hayTexto("Arriendo") }
        compose.onNodeWithText("Sueldo").assertIsDisplayed()

        compose.onNodeWithText("Gastos").performClick()
        compose.esperarHasta("que se oculte el sueldo") { !hayTexto("Sueldo") }
        compose.onNodeWithText("Arriendo").assertIsDisplayed()
    }

    @Test fun indicadoresSeMuestranYConviertenElSaldo() {
        api.respuesta = {
            listOf(
                IndicadorDto("dolar", "Dólar observado", "Pesos", "2026-10-26T03:00:00.000Z", 1000.0),
                IndicadorDto("uf", "Unidad de fomento (UF)", "Pesos", "2026-10-26T03:00:00.000Z", 41221.08),
            )
        }
        abrirApp()

        // El Inicio pide indicadores al abrir: saldo $744.800 / 1000 = US$744,80.
        // Se usa el mismo formateador que la app (NumberFormat puede usar un
        // espacio no separable U+00A0 entre el símbolo y el número).
        compose.esperarTexto("≈ ${formatearDolares(744.8)} al dólar observado")

        compose.onNodeWithText("Indicadores").performClick()
        compose.esperarTexto("Unidad de fomento (UF)")
        compose.onNodeWithText(formatearValorIndicador(41221.08)).assertIsDisplayed()
    }

    @Test fun sinConexionYSinCacheMuestraErrorConReintentar() {
        api.respuesta = { throw IOException("sin red") }
        abrirApp()
        compose.onNodeWithText("Indicadores").performClick()

        compose.esperarTexto("Sin conexión a internet")
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test fun laAppArrancaBloqueadaYSeDesbloqueaAlAutenticarse() {
        autenticador.error = "Huella no reconocida"
        abrirApp()

        compose.esperarTexto("Control de Gastos está bloqueado")
        compose.onNodeWithText("Huella no reconocida").assertIsDisplayed()
        // Bloqueada: los datos de la app no son accesibles (ni para TalkBack).
        compose.onNodeWithText("Saldo disponible").assertDoesNotExist()

        autenticador.error = null
        compose.onNodeWithText("Desbloquear").performClick()

        compose.esperarTexto("Saldo disponible")
        compose.onNodeWithText("Control de Gastos está bloqueado").assertDoesNotExist()
    }

    private fun ComposeTestRule.esperarTexto(texto: String) =
        esperarHasta("el texto \"$texto\"") { hayTexto(texto) }

    /**
     * Espera a que se cumpla `condicion` con datos que llegan desde otro hilo
     * (Room, red): sincroniza con la UI (`waitForIdle`) y vuelve a mirar, hasta
     * agotar el tiempo. Si falla, vuelca el árbol de la UI a Logcat (etiqueta
     * "ArbolUI") para ver qué había realmente en pantalla.
     */
    private fun ComposeTestRule.esperarHasta(
        descripcion: String,
        timeoutMs: Long = 5_000,
        condicion: ComposeTestRule.() -> Boolean,
    ) {
        val limite = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            waitForIdle()
            if (condicion()) return
            if (SystemClock.uptimeMillis() > limite) {
                onRoot(useUnmergedTree = true).printToLog("ArbolUI")
                throw AssertionError("Se agotó la espera de $descripcion")
            }
            SystemClock.sleep(50)
        }
    }

    private fun ComposeTestRule.hayTexto(texto: String) =
        onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
}
