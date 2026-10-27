package com.example.controlgastos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// version: subirla cada vez que cambie el esquema (y agregar una migración,
// si no los usuarios perderían sus datos). El esquema exportado queda en
// app/schemas/.
@Database(
    entities = [
        GastoEntity::class,
        CategoriaEntity::class,
        TotalMesEntity::class,
        IngresoEntity::class,
        IndicadorEntity::class,
    ],
    // v1: Fases 3-5 (tabla "cotizaciones", dólar argentino).
    // v2: tabla "indicadores" (indicadores de Chile). Ver MIGRACION_1_2.
    // v3: gastos. — gastos y categorías de la cuenta (caché) e ingresos locales. Ver MIGRACION_2_3.
    version = 3,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class GastosDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun totalMesDao(): TotalMesDao
    abstract fun ingresoDao(): IngresoDao
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
            // cifrada: se descarta. En una app real se migraría con
            // sqlcipher_export (ver docs/FASE-5).
            context.deleteDatabase(NOMBRE_SIN_CIFRAR)
            return Room.databaseBuilder(context, GastosDatabase::class.java, NOMBRE)
                .openHelperFactory(SupportOpenHelperFactory(clave))
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .build()
        }

        /** Para tests: BD en memoria (sin cifrar), vive solo mientras corre el proceso. */
        fun construirEnMemoria(context: Context): GastosDatabase =
            Room.inMemoryDatabaseBuilder(context, GastosDatabase::class.java).build()
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

/**
 * Migración de la versión 2 a la 3 (la app pasa a usar la cuenta de gastos.):
 * - Los gastos ahora viven en el servidor: se crean las tablas de caché.
 * - Los INGRESOS que el usuario tenía en "movimientos" (categorías SUELDO y
 *   EXTRA) se copian a la nueva tabla "ingresos": son datos que solo existen
 *   en este teléfono y no se deben perder.
 * - Los gastos locales viejos se descartan: la fuente de verdad es la cuenta.
 * El SQL de CREATE es el que Room genera (app/schemas/.../3.json);
 * MigracionTest verifica que coincida.
 */
val MIGRACION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `gastos` (`id` INTEGER NOT NULL, `mes` TEXT NOT NULL, `monto` REAL NOT NULL, " +
                "`nota` TEXT, `fecha` INTEGER NOT NULL, `origen` TEXT NOT NULL, `categoriaId` INTEGER NOT NULL, " +
                "`categoriaNombre` TEXT NOT NULL, `categoriaIcono` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gastos_mes` ON `gastos` (`mes`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gastos_fecha` ON `gastos` (`fecha`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categorias` (`id` INTEGER NOT NULL, `nombre` TEXT NOT NULL, " +
                "`icono` TEXT NOT NULL, `orden` INTEGER NOT NULL, `archivada` INTEGER NOT NULL, " +
                "`usos` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `totales_mes` (`mes` TEXT NOT NULL, `total` REAL NOT NULL, PRIMARY KEY(`mes`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ingresos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`monto` REAL NOT NULL, `tipo` TEXT NOT NULL, `nota` TEXT, `fecha` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingresos_fecha` ON `ingresos` (`fecha`)")
        db.execSQL(
            "INSERT INTO `ingresos` (`monto`, `tipo`, `nota`, `fecha`) " +
                "SELECT `monto`, CASE `categoria` WHEN 'SUELDO' THEN 'SUELDO' ELSE 'OTRO' END, `descripcion`, `fecha` " +
                "FROM `movimientos` WHERE `categoria` IN ('SUELDO', 'EXTRA')",
        )
        db.execSQL("DROP TABLE IF EXISTS `movimientos`")
    }
}
