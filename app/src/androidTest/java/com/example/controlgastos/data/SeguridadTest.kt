package com.example.controlgastos.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.seguridad.GestorClaves
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Se ejecuta en el dispositivo: el Keystore y SQLCipher no existen en la JVM.
class SeguridadTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val nombreBd = "gastos_cifrada.db"

    @Before @After fun limpiar() {
        context.deleteDatabase(nombreBd)
    }

    @Test fun cifrarYDescifrarConElKeystore() {
        val claves = GestorClaves(context)
        val secreto = "token-de-prueba".toByteArray()

        val cifrado = claves.cifrar(secreto)

        assertFalse("el texto cifrado no debe contener el original", cifrado.contains("token"))
        assertArrayEquals(secreto, claves.descifrar(cifrado))
        // Mismo dato, IV distinto: dos cifrados nunca son iguales.
        assertNotEquals(cifrado, claves.cifrar(secreto))
    }

    @Test fun laClaveDeLaBdEsEstable() {
        val primera = GestorClaves(context).claveBaseDeDatos()
        // Otra instancia (como tras reiniciar la app) recupera la MISMA clave.
        val segunda = GestorClaves(context).claveBaseDeDatos()

        assertEquals(32, primera.size)
        assertArrayEquals(primera, segunda)
    }

    @Test fun laBdCifradaSoloAbreConSuClave() = runTest {
        val clave = GestorClaves(context).claveBaseDeDatos()
        GastosDatabase.construirCifrada(context, clave).apply {
            assertEquals(9, movimientoDao().observarTodos().first().size)
            close()
        }

        // El archivo en disco no es un SQLite legible: no empieza con la
        // cabecera "SQLite format 3" de las bases sin cifrar.
        val cabecera = context.getDatabasePath(nombreBd).readBytes().copyOf(15).decodeToString()
        assertNotEquals("SQLite format 3", cabecera)

        // Con otra clave, no se puede abrir.
        val claveIncorrecta = ByteArray(32) { 7 }
        val fallo = runCatching {
            GastosDatabase.construirCifrada(context, claveIncorrecta).apply {
                movimientoDao().observarTodos().first()
                close()
            }
        }
        assertTrue("debía fallar con una clave incorrecta", fallo.isFailure)
    }
}
