package com.example.controlgastos

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ActivityScenario
import com.example.controlgastos.data.remote.aModelo
import com.example.controlgastos.data.seguridad.AlmacenSesion
import com.example.controlgastos.ui.formatearDinero
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import javax.inject.Inject

// Prueba de UI de la app COMPLETA (MainActivity + Hilt + ViewModels + Room en
// memoria), con el backend reemplazado por FakeGastosApi.
// ./gradlew :app:connectedDebugAndroidTest
@HiltAndroidTest
class NavegacionTest {

    private val hilt = HiltAndroidRule(this)
    // "Empty": la regla no lanza ninguna Activity; cada test la lanza cuando ya
    // preparó la sesión y las APIs falsas.
    private val compose = createEmptyComposeRule()

    // Hilt primero (crea el grafo de dependencias), luego Compose.
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(hilt).around(compose)

    @Inject lateinit var api: FakeGastosApi
    @Inject lateinit var autenticador: FakeAutenticador
    @Inject lateinit var almacen: AlmacenSesion

    private var escenario: ActivityScenario<MainActivity>? = null

    @Before fun preparar() {
        hilt.inject()
        // La sesión se guarda en el dispositivo y sobrevive entre tests: cada uno parte limpio.
        almacen.borrar()
    }

    @After fun cerrar() {
        escenario?.close()
        almacen.borrar()
    }

    private fun abrirApp(conSesion: Boolean = true) {
        if (conSesion) almacen.guardar("1.9999999999999.firma", FakeGastosApi.USUARIO.aModelo())
        escenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun boton(texto: String) = compose.onNode(hasText(texto) and hasClickAction())

    /**
     * Desplaza hasta el elemento y ejecuta su acción de clic (la misma que usa
     * TalkBack). Un toque en coordenadas podría caer en la navegación flotante,
     * que se dibuja ENCIMA del final de la pantalla.
     */
    private fun SemanticsNodeInteraction.pulsar() {
        // Solo si está dentro de algo desplazable (la pantalla de bloqueo no lo está).
        runCatching { performScrollTo() }
        performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test fun iniciarSesionLlevaAlInicio() {
        abrirApp(conSesion = false)
        compose.esperarTexto("Inicia sesión")

        compose.onNodeWithContentDescription("Email").performTextInput(FakeGastosApi.EMAIL)
        compose.onNodeWithContentDescription("Contraseña").performTextInput("incorrecta")
        boton("Iniciar sesión").pulsar()
        compose.esperarTexto("Email o contraseña incorrectos")

        compose.onNodeWithContentDescription("Contraseña").performTextReplacement(FakeGastosApi.CONTRASENA)
        boton("Iniciar sesión").pulsar()

        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.onNodeWithText("Hola, Ana · 0 movimientos este mes").assertIsDisplayed()
    }

    @Test fun registrarVerYEliminarUnGasto() {
        abrirApp()
        compose.esperarTexto("Este mes llevas", sustring = true)

        compose.onNodeWithContentDescription("Nuevo gasto").pulsar()
        compose.esperarTexto("¿Cuánto gastaste?")
        boton("Registrar gasto").pulsar()
        compose.esperarTexto("Ingresa un monto válido")

        compose.onNodeWithContentDescription("Monto").performTextInput("12500")
        compose.onNodeWithText("12.500").assertIsDisplayed() // formatea mientras se escribe
        compose.onNodeWithText("Supermercado").pulsar()
        compose.onNodeWithContentDescription("Nota").performTextInput("Feria del domingo")
        boton("Registrar gasto").pulsar()

        // Vuelve al Inicio con el total actualizado ("Este mes llevas / $12.500.").
        compose.esperarTexto("${formatearDinero(12500.0, "CLP")}.", sustring = true)
        compose.desplazarHasta("Feria del domingo")

        compose.onNodeWithText("Feria del domingo").pulsar()
        compose.esperarTexto("Registrado desde Android")
        boton("Eliminar gasto").pulsar()
        compose.onNodeWithText("Eliminar").pulsar()

        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.esperarHasta("que desaparezca el gasto") { !hayTexto("Feria del domingo") }
        check(api.gastos.isEmpty()) { "El gasto debía borrarse también en el servidor" }
    }

    @Test fun registrarUnIngresoLocal() {
        abrirApp()
        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.onNodeWithContentDescription("Nuevo gasto").pulsar()
        compose.esperarTexto("¿Cuánto gastaste?")

        compose.onNodeWithText("Ingreso").pulsar()
        compose.esperarTexto("¿Cuánto recibiste?")
        compose.onNodeWithContentDescription("Monto").performTextInput("850000")
        compose.onNodeWithText("Sueldo").pulsar()
        boton("Registrar ingreso").pulsar()

        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.desplazarHasta("+${formatearDinero(850000.0, "CLP")}")
        check(api.gastos.isEmpty()) { "Un ingreso no debe enviarse al servidor" }
    }

    @Test fun sinConexionMuestraLoGuardadoConAviso() {
        api.sinConexion = true
        abrirApp()
        compose.esperarTexto("Sin conexión a internet. Mostrando datos guardados.")
        compose.onNodeWithText("Este mes llevas", substring = true).assertIsDisplayed()
    }

    @Test fun cerrarSesionVuelveAlLogin() {
        abrirApp()
        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.onNodeWithContentDescription("Ajustes").pulsar()
        compose.esperarTexto("a tu manera.", sustring = true)

        compose.desplazarHasta("Cerrar sesión")
        boton("Cerrar sesión").pulsar()
        // El diálogo se dibuja en su propia ventana, después de la pantalla: es el último botón.
        compose.onAllNodes(hasText("Cerrar sesión") and hasClickAction()).onLast().pulsar()

        compose.esperarTexto("Inicia sesión")
    }

    @Test fun laAppArrancaBloqueadaYSeDesbloqueaAlAutenticarse() {
        autenticador.error = "Huella no reconocida"
        abrirApp()

        compose.esperarTexto("gastos. está bloqueado")
        compose.onNodeWithText("Huella no reconocida").assertIsDisplayed()
        // Bloqueada: los datos de la app no son accesibles (ni para TalkBack).
        compose.onNodeWithText("Este mes llevas", substring = true).assertDoesNotExist()

        autenticador.error = null
        boton("Desbloquear").pulsar()

        compose.esperarTexto("Este mes llevas", sustring = true)
        compose.onNodeWithText("gastos. está bloqueado").assertDoesNotExist()
    }

    private fun ComposeTestRule.esperarTexto(texto: String, sustring: Boolean = false) =
        esperarHasta("el texto \"$texto\"") { hayTexto(texto, sustring) }

    /**
     * Desplaza la pantalla hasta el elemento con `texto`: así el test no
     * depende del tamaño de pantalla (en el CI el emulador es más chico).
     */
    private fun ComposeTestRule.desplazarHasta(texto: String) =
        esperarHasta("el elemento \"$texto\"") {
            runCatching { onNode(hasScrollAction()).performScrollToNode(hasText(texto)) }.isSuccess
        }

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

    private fun ComposeTestRule.hayTexto(texto: String, sustring: Boolean = false) =
        onAllNodesWithText(texto, substring = sustring).fetchSemanticsNodes().isNotEmpty()
}
