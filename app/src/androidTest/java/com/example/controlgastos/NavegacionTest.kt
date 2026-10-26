package com.example.controlgastos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.controlgastos.ui.navegacion.GastosApp
import com.example.controlgastos.ui.theme.ControlGastosTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Prueba de UI: se ejecuta en un emulador o dispositivo.
// ./gradlew :app:connectedDebugAndroidTest
class NavegacionTest {

    @get:Rule val compose = createComposeRule()

    @Before fun iniciar() {
        compose.setContent { ControlGastosTheme { GastosApp() } }
    }

    @Test fun agregarVerYEliminarUnGasto() {
        // Inicio -> formulario
        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithText("Nuevo movimiento").assertIsDisplayed()

        // Guardar vacío muestra errores de validación
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Escribe una descripción").assertIsDisplayed()

        // Completar y guardar
        compose.onNodeWithText("Descripción").performTextInput("Café de prueba")
        compose.onNodeWithText("Monto").performTextInput("1234,5")
        compose.onNodeWithText("🎬 Ocio").performClick()
        compose.onNodeWithText("Guardar").performClick()

        // Vuelve a Inicio y aparece primero en "Últimos movimientos"
        compose.onNodeWithText("Café de prueba").assertIsDisplayed().performClick()

        // Detalle -> eliminar con confirmación
        compose.onNodeWithText("Detalle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Eliminar").performClick()
        compose.onNodeWithText("Eliminar").performClick()

        compose.onNodeWithText("Control de Gastos").assertIsDisplayed()
        compose.onNodeWithText("Café de prueba").assertDoesNotExist()
    }

    @Test fun filtrarMovimientosPorTipo() {
        compose.onNodeWithText("Movimientos").performClick()

        compose.onNodeWithText("Ingresos").performClick()
        compose.onNodeWithText("Sueldo de octubre").assertIsDisplayed()
        compose.onNodeWithText("Alquiler").assertDoesNotExist()

        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithText("Alquiler").assertIsDisplayed()
        compose.onNodeWithText("Sueldo de octubre").assertDoesNotExist()
    }
}
