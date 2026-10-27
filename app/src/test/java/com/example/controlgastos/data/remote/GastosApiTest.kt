package com.example.controlgastos.data.remote

import com.example.controlgastos.domain.repository.ErrorApp
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Prueba el cliente de la API de gastos. con Retrofit REAL contra un servidor
 * HTTP falso: formato del JSON (el mismo que devuelven las rutas /api/app/...),
 * el header Authorization y el manejo de errores.
 */
class GastosApiTest {

    private val servidor = MockWebServer()
    private var token: String? = "1.9999999999999.firma"
    private var sesionesExpiradas = 0
    private lateinit var api: GastosApi

    @Before fun iniciar() {
        servidor.start()
        api = crearGastosApi(servidor.url("/").toString(), token = { token }, sesionExpirada = { sesionesExpiradas++ })
    }

    @After fun terminar() = servidor.close()

    private fun responder(codigo: Int, cuerpo: String) =
        servidor.enqueue(MockResponse.Builder().code(codigo).addHeader("content-type", "application/json").body(cuerpo).build())

    @Test fun `login envia email y contrasena y lee la sesion`() = runTest {
        token = null
        responder(
            200,
            """{"token":"7.123.abc","user":{"id":7,"name":"Lorenzo","email":"l@e.cl","currency":"CLP","timezone":"America/Santiago","avatarVersion":null}}""",
        )

        val sesion = api.iniciarSesion(LoginBody("l@e.cl", "secreta123"))

        val peticion = servidor.takeRequest()
        assertEquals("/api/app/auth/login", peticion.url.encodedPath)
        assertNull("sin sesión no se envía Authorization", peticion.headers["Authorization"])
        assertTrue(peticion.body!!.utf8().contains("\"password\":\"secreta123\""))
        assertEquals("7.123.abc", sesion.token)
        assertEquals("Lorenzo", sesion.user.aModelo().nombre)
    }

    @Test fun `con sesion envia el token Bearer y lee los gastos del mes`() = runTest {
        responder(
            200,
            """{"month":"2026-10","expenses":[{"id":3,"amount":12500.5,"note":null,"spentOn":"2026-10-09","source":"shortcut",
               "categoryId":1,"categoryName":"Comida","categoryIcon":"utensils","campoNuevo":"se ignora"}]}""",
        )

        val gasto = api.gastos("2026-10").expenses.single().aModelo()

        val peticion = servidor.takeRequest()
        assertEquals("Bearer 1.9999999999999.firma", peticion.headers["Authorization"])
        assertEquals("mes=2026-10", peticion.url.encodedQuery)
        assertEquals(12500.5, gasto.monto, 0.0)
        assertEquals(LocalDate.of(2026, 10, 9), gasto.fecha)
        assertNull(gasto.nota)
        assertEquals("shortcut", gasto.origen)
    }

    @Test fun `un 401 con token avisa que la sesion expiro`() = runTest {
        responder(401, """{"error":"Tu sesión expiró. Vuelve a iniciar sesión."}""")

        val resultado = llamarApi { api.yo() }

        val error = resultado.exceptionOrNull() as ErrorApp
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", error.mensaje)
        assertTrue(error.sesionExpirada)
        assertEquals(1, sesionesExpiradas)
    }

    @Test fun `un 401 del login (sin token) no cierra ninguna sesion`() = runTest {
        token = null
        responder(401, """{"error":"Email o contraseña incorrectos"}""")

        val error = llamarApi { api.iniciarSesion(LoginBody("x@y.cl", "mala")) }.exceptionOrNull() as ErrorApp

        assertEquals("Email o contraseña incorrectos", error.mensaje)
        assertEquals(0, sesionesExpiradas)
    }

    @Test fun `los errores de validacion muestran el mensaje del backend`() = runTest {
        responder(400, """{"error":"Categoría inválida"}""")
        val error = llamarApi { api.crearGasto(GastoBody(1000.0, 99, "2026-10-10", null)) }.exceptionOrNull() as ErrorApp
        assertEquals("Categoría inválida", error.mensaje)
        assertFalse(error.sesionExpirada)
    }

    @Test fun `editar una categoria solo envia los campos que cambian`() = runTest {
        responder(200, """{"id":2,"name":"Hogar","icon":"house","sort":1,"archived":true}""")

        api.editarCategoria(2, CategoriaBody(archived = true))

        assertEquals("""{"archived":true}""", servidor.takeRequest().body!!.utf8())
    }

    @Test fun `servidor caido es sin conexion`() = runTest {
        servidor.close()
        val error = llamarApi { api.categorias() }.exceptionOrNull() as ErrorApp
        assertEquals("Sin conexión a internet", error.mensaje)
    }

    @Test fun `una fecha con formato inesperado es respuesta invalida`() = runTest {
        responder(200, """{"month":"2026-10","expenses":[{"id":1,"amount":1,"spentOn":"ayer","categoryId":1,"categoryName":"x","categoryIcon":"x"}]}""")
        val error = llamarApi { api.gastos("2026-10").expenses.map { it.aModelo() } }.exceptionOrNull() as ErrorApp
        assertEquals("La respuesta del servidor no es válida", error.mensaje)
    }
}
