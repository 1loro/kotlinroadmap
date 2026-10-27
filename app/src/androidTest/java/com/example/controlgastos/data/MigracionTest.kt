package com.example.controlgastos.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MIGRACION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Prueba la migración 1 -> 2 con los esquemas exportados en app/schemas/
 * (el plugin de Room los agrega como assets de los tests instrumentados).
 */
class MigracionTest {

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), GastosDatabase::class.java)

    private val nombre = "migracion-test.db"

    @Test fun migrar1a2ConservaLosMovimientosYCreaIndicadores() {
        // 1. BD en la versión 1, como la tenía un usuario de las fases 3-5.
        helper.createDatabase(nombre, 1).apply {
            insert(
                "movimientos", SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("descripcion", "Arriendo")
                    put("monto", 450_000.0)
                    put("categoria", "VIVIENDA")
                    put("fecha", 20_000L)
                },
            )
            insert(
                "cotizaciones", SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("casa", "oficial")
                    put("nombre", "Oficial")
                    put("venta", 1560.0)
                    put("actualizada", 0L)
                    put("orden", 0)
                },
            )
            close()
        }

        // 2. Migrar. `validateDroppedTables = true` + el esquema 2.json: falla si
        //    el resultado no es EXACTAMENTE lo que Room espera para la versión 2.
        val db = helper.runMigrationsAndValidate(nombre, 2, true, MIGRACION_1_2)

        // 3. Los datos del usuario siguen ahí.
        db.query("SELECT descripcion, monto FROM movimientos").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Arriendo", cursor.getString(0))
            assertEquals(450_000.0, cursor.getDouble(1), 0.0)
        }
        // 4. La caché vieja ya no existe.
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'cotizaciones'").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
    }
}
