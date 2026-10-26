package com.example.controlgastos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.controlgastos.model.Categoria
import com.example.controlgastos.model.Cotizacion
import com.example.controlgastos.model.Movimiento
import java.time.Instant
import java.time.LocalDate

// Las entidades describen TABLAS. Se mantienen separadas del modelo de la app
// (model/) para que un cambio en la BD no obligue a tocar la UI, y viceversa.

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    // autoGenerate: SQLite asigna el id al insertar (pasamos 0).
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descripcion: String,
    val monto: Double,
    // Room guarda los enum como TEXT con su nombre ("COMIDA").
    val categoria: Categoria,
    val fecha: LocalDate,
)

@Entity(tableName = "cotizaciones")
data class CotizacionEntity(
    @PrimaryKey val casa: String,
    val nombre: String,
    val compra: Double?,
    val venta: Double?,
    val actualizada: Instant,
    // Posición original en la respuesta de la API, para mostrarlas en ese orden.
    val orden: Int,
)

// SQLite solo conoce números, texto y blobs: los TypeConverter traducen
// los tipos de Kotlin que no entiende.
class Convertidores {
    @TypeConverter fun fechaANumero(fecha: LocalDate): Long = fecha.toEpochDay()
    @TypeConverter fun numeroAFecha(dias: Long): LocalDate = LocalDate.ofEpochDay(dias)

    @TypeConverter fun instanteANumero(instante: Instant): Long = instante.toEpochMilli()
    @TypeConverter fun numeroAInstante(millis: Long): Instant = Instant.ofEpochMilli(millis)
}

// --- Mapeos entre entidades y modelo ---

fun MovimientoEntity.aModelo() = Movimiento(id, descripcion, monto, categoria, fecha)

fun Movimiento.aEntidad() = MovimientoEntity(id, descripcion, monto, categoria, fecha)

fun CotizacionEntity.aModelo() = Cotizacion(casa, nombre, compra, venta, actualizada)
