package com.example.controlgastos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.controlgastos.data.DatosDeEjemplo
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// version: subirla cada vez que cambie el esquema (y agregar una migración,
// si no los usuarios perderían sus datos). El esquema exportado queda en
// app/schemas/.
@Database(
    entities = [MovimientoEntity::class, IndicadorEntity::class],
    // v1: Fases 3-5 (tabla "cotizaciones", dólar argentino).
    // v2: tabla "indicadores" (indicadores de Chile). Ver MIGRACION_1_2.
    version = 2,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class GastosDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun indicadorDao(): IndicadorDao

    companion object {
        private const val NOMBRE = "gastos_cifrada.db"
        // Archivo de las fases 3 y 4, sin cifrar (ver construirCifrada).
        private const val NOMBRE_SIN_CIFRAR = "gastos.db"

        /**
         * BD real de la app, cifrada con SQLCipher (AES-256). Sin la clave, el
         * archivo es ilegible aunque alguien lo copie del teléfono (root,
         * backup, teléfono robado).
         *
         * @param clave 32 bytes aleatorios protegidos por el Keystore (GestorClaves).
         */
        fun construirCifrada(context: Context, clave: ByteArray): GastosDatabase {
            System.loadLibrary("sqlcipher")
            // La BD sin cifrar de versiones anteriores no puede abrirse como
            // cifrada: se descarta y se empieza con los datos de ejemplo. En
            // una app real se migraría con sqlcipher_export (ver docs/FASE-5).
            context.deleteDatabase(NOMBRE_SIN_CIFRAR)
            return Room.databaseBuilder(context, GastosDatabase::class.java, NOMBRE)
                .openHelperFactory(SupportOpenHelperFactory(clave))
                .addMigrations(MIGRACION_1_2)
                .addCallback(SembrarDatosDeEjemplo)
                .build()
        }

        /** Para tests: BD en memoria (sin cifrar), vive solo mientras corre el proceso. */
        fun construirEnMemoria(context: Context): GastosDatabase =
            Room.inMemoryDatabaseBuilder(context, GastosDatabase::class.java)
                .addCallback(SembrarDatosDeEjemplo)
                .build()
    }
}

/**
 * Migración de la versión 1 a la 2: sin ella, Room no puede abrir la BD de
 * quien ya tenía la app instalada.
 *
 * La tabla vieja "cotizaciones" era solo una CACHÉ de la API (se vuelve a
 * descargar), así que se puede descartar. La tabla "movimientos", con los
 * datos del usuario, NO se toca. El SQL de CREATE TABLE es el que Room genera
 * para IndicadorEntity (copiado de app/schemas/.../2.json); MigracionTest
 * verifica que coincida.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `cotizaciones`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `indicadores` (`codigo` TEXT NOT NULL, `nombre` TEXT NOT NULL, " +
                "`valor` REAL NOT NULL, `fecha` INTEGER NOT NULL, `orden` INTEGER NOT NULL, PRIMARY KEY(`codigo`))",
        )
    }
}

// Se ejecuta UNA vez, cuando se crea el archivo de la BD (primera apertura de
// la app), para que no arranque vacía. Si el usuario borra todo, no se repite.
private object SembrarDatosDeEjemplo : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val convertidores = Convertidores()
        DatosDeEjemplo.movimientos().forEach { m ->
            val valores = ContentValues().apply {
                put("descripcion", m.descripcion)
                put("monto", m.monto)
                put("categoria", m.categoria.name)
                put("fecha", convertidores.fechaANumero(m.fecha))
            }
            db.insert("movimientos", SQLiteDatabase.CONFLICT_ABORT, valores)
        }
    }
}
