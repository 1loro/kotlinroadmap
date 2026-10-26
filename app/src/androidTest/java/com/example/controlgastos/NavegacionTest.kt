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
import com.example.controlgastos.data.remote.CotizacionDto
import com.example.controlgastos.ui.formatearDolares
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
// en memoria), con la red reemplazada por FakeDolarApi.
// ./gradlew :app:connectedDebugAndroidTest
@HiltAndroidTest
class NavegacionTest {

    private val hilt = HiltAndroidRule(this)
    // "Empty": la regla no lanza ninguna Activity; cada test la lanza cuando
    // ya configuró la API falsa (si no, la app pediría cotizaciones antes).
    private val compose = createEmptyComposeRule()

    // Hilt primero (crea el grafo de dependencias), luego Compose.
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(hilt).around(compose)

    @Inject lateinit var api: FakeDolarApi
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
        compose.esperarTexto("Verdulería")

        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithText("Guardar").performClick()
        compose.esperarTexto("Escribe una descripción")

        compose.onNodeWithText("Descripción").performTextInput("Café de prueba")
        compose.onNodeWithText("Monto").performTextInput("1234,5")
        compose.onNodeWithText("🎬 Ocio").performClick()
        compose.onNodeWithText("Guardar").performClick()

        compose.esperarTexto("Café de prueba")
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
        compose.esperarTexto("Alquiler")

        compose.onNodeWithText("Ingresos").performClick()
        compose.esperarHasta("que se oculte Alquiler") { !hayTexto("Alquiler") }
        compose.onNodeWithText("Sueldo de octubre").assertIsDisplayed()

        compose.onNodeWithText("Gastos").performClick()
        compose.esperarHasta("que se oculte el sueldo") { !hayTexto("Sueldo de octubre") }
        compose.onNodeWithText("Alquiler").assertIsDisplayed()
    }

    @Test fun cotizacionesSeMuestranYConvierteElSaldo() {
        api.respuesta = {
            listOf(
                CotizacionDto("oficial", "Oficial", 1000.0, 1000.0, "2026-10-26T16:00:00.000Z"),
                CotizacionDto("blue", "Blue", 1500.0, 1550.0, "2026-10-26T19:00:00.000Z"),
            )
        }
        abrirApp()

        // El Inicio pide cotizaciones al abrir: saldo $ 437.200 / 1000 = US$ 437,20.
        // Se usa el mismo formateador que la app: NumberFormat separa "US$" del
        // número con un espacio no separable (U+00A0).
        compose.esperarTexto("≈ ${formatearDolares(437.2)} al dólar oficial")

        compose.onNodeWithText("Dólar").performClick()
        compose.esperarTexto("Blue")
        compose.onNodeWithText("Oficial").assertIsDisplayed()
    }

    @Test fun sinConexionYSinCacheMuestraErrorConReintentar() {
        api.respuesta = { throw IOException("sin red") }
        abrirApp()
        compose.onNodeWithText("Dólar").performClick()

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
