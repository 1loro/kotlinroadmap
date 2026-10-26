package com.example.controlgastos

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import com.example.controlgastos.data.AppContainer
import com.example.controlgastos.data.CotizacionesRepository
import com.example.controlgastos.data.MovimientosRepository
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.remote.CotizacionDto
import com.example.controlgastos.data.remote.DolarApi
import com.example.controlgastos.ui.formatearDolares
import com.example.controlgastos.ui.navegacion.GastosApp
import com.example.controlgastos.ui.theme.ControlGastosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.IOException

// Prueba de UI: se ejecuta en un emulador o dispositivo.
// ./gradlew :app:connectedDebugAndroidTest
class NavegacionTest {

    private val compose = createComposeRule()

    // La BD vive en una regla propia que ENVUELVE a la de Compose: se abre antes
    // que la UI y se cierra después de destruirla (con un @After se cerraría
    // mientras la pantalla todavía escucha sus Flows).
    private val reglaDb = object : ExternalResource() {
        lateinit var db: GastosDatabase
        override fun before() {
            db = GastosDatabase.construir(ApplicationProvider.getApplicationContext(), enMemoria = true)
        }
        override fun after() = db.close()
    }

    @get:Rule val reglas: RuleChain = RuleChain.outerRule(reglaDb).around(compose)

    /** API falsa: devuelve lo que diga `respuesta`, o lanza su excepción. */
    private class FakeDolarApi(val respuesta: () -> List<CotizacionDto>) : DolarApi {
        override suspend fun obtenerDolares() = respuesta()
    }

    private fun iniciar(api: DolarApi = FakeDolarApi { emptyList() }) {
        val db = reglaDb.db
        val container = AppContainer(
            MovimientosRepository(db.movimientoDao()),
            CotizacionesRepository(api, db.cotizacionDao()),
        )
        compose.setContent { ControlGastosTheme { GastosApp(container) } }
    }

    @Test fun agregarVerYEliminarUnGasto() {
        iniciar()
        compose.esperarTexto("Verdulería")

        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Escribe una descripción").assertIsDisplayed()

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
        iniciar()
        compose.onNodeWithText("Movimientos").performClick()
        compose.esperarTexto("Alquiler")

        compose.onNodeWithText("Ingresos").performClick()
        compose.onNodeWithText("Sueldo de octubre").assertIsDisplayed()
        compose.onNodeWithText("Alquiler").assertDoesNotExist()

        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithText("Alquiler").assertIsDisplayed()
        compose.onNodeWithText("Sueldo de octubre").assertDoesNotExist()
    }

    @Test fun cotizacionesSeMuestranYConvierteElSaldo() {
        iniciar(
            FakeDolarApi {
                listOf(
                    CotizacionDto("oficial", "Oficial", 1000.0, 1000.0, "2026-10-26T16:00:00.000Z"),
                    CotizacionDto("blue", "Blue", 1500.0, 1550.0, "2026-10-26T19:00:00.000Z"),
                )
            },
        )
        compose.onNodeWithText("Dólar").performClick()
        compose.esperarTexto("Blue")
        compose.onNodeWithText("Oficial").assertIsDisplayed()

        // Datos de ejemplo: saldo $ 437.200 / 1000 = US$ 437,20. Se usa el mismo
        // formateador que la app: NumberFormat separa "US$" del número con un
        // espacio no separable (U+00A0), distinto del espacio común.
        compose.onNodeWithText("Inicio").performClick()
        compose.esperarTexto("≈ ${formatearDolares(437.2)} al dólar oficial")
    }

    @Test fun sinConexionYSinCacheMuestraErrorConReintentar() {
        iniciar(FakeDolarApi { throw IOException("sin red") })
        compose.onNodeWithText("Dólar").performClick()

        compose.esperarTexto("Sin conexión a internet")
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    private fun ComposeContentTestRule.esperarTexto(texto: String) =
        esperarHasta("el texto \"$texto\"") { hayTexto(texto) }

    /**
     * Espera a que se cumpla `condicion` con datos que llegan desde otro hilo
     * (Room, red): sincroniza con la UI (`waitForIdle`) y vuelve a mirar, hasta
     * agotar el tiempo. Si falla, vuelca el árbol de la UI a Logcat (etiqueta
     * "ArbolUI") para ver qué había realmente en pantalla.
     */
    private fun ComposeContentTestRule.esperarHasta(
        descripcion: String,
        timeoutMs: Long = 5_000,
        condicion: ComposeContentTestRule.() -> Boolean,
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

    private fun ComposeContentTestRule.hayTexto(texto: String) =
        onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
}
