package com.example.controlgastos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.controlgastos.data.DatosDeEjemplo

// version: subirla cada vez que cambie el esquema (y agregar una migración,
// si no los usuarios perderían sus datos). El esquema exportado queda en
// app/schemas/.
@Database(
    entities = [MovimientoEntity::class, CotizacionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class GastosDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun cotizacionDao(): CotizacionDao

    companion object {
        private const val NOMBRE = "gastos.db"

        /** @param enMemoria true para tests: la BD vive solo mientras corre el proceso. */
        fun construir(context: Context, enMemoria: Boolean = false): GastosDatabase {
            val builder = if (enMemoria) {
                Room.inMemoryDatabaseBuilder(context, GastosDatabase::class.java)
            } else {
                Room.databaseBuilder(context, GastosDatabase::class.java, NOMBRE)
            }
            return builder.addCallback(SembrarDatosDeEjemplo).build()
        }
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
