package com.example.controlgastos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.model.TotalMes
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// Las entidades describen TABLAS. Se mantienen separadas del modelo de la app
// (domain/model/) para que un cambio en la BD no obligue a tocar la UI.
//
// gastos, categorias y totales_mes son una CACHÉ de la cuenta en el servidor
// (se borran al cerrar sesión). ingresos es la única tabla con datos que
// existen solo en este teléfono.

@Entity(tableName = "gastos", indices = [Index("mes"), Index("fecha")])
data class GastoEntity(
    // Id del servidor.
    @PrimaryKey val id: Long,
    /** "AAAA-MM": para reemplazar de una vez la caché de un mes. */
    val mes: String,
    val monto: Double,
    val nota: String?,
    val fecha: LocalDate,
    val origen: String,
    val categoriaId: Long,
    val categoriaNombre: String,
    val categoriaIcono: String,
)

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val icono: String,
    val orden: Int,
    val archivada: Boolean,
    val usos: Int,
)

@Entity(tableName = "totales_mes")
data class TotalMesEntity(
    /** "AAAA-MM". */
    @PrimaryKey val mes: String,
    val total: Double,
)

@Entity(tableName = "ingresos", indices = [Index("fecha")])
data class IngresoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monto: Double,
    // Room guarda los enum como TEXT con su nombre ("SUELDO").
    val tipo: TipoIngreso,
    val nota: String?,
    val fecha: LocalDate,
)

@Entity(tableName = "indicadores")
data class IndicadorEntity(
    @PrimaryKey val codigo: String,
    val nombre: String,
    val valor: Double,
    val fecha: Instant,
    // Orden en que se muestran (dólar, euro, UF, UTM).
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

fun GastoEntity.aModelo() = Gasto(id, monto, nota, fecha, origen, categoriaId, categoriaNombre, categoriaIcono)

fun Gasto.aEntidad() =
    GastoEntity(id, mes.toString(), monto, nota, fecha, origen, categoriaId, categoriaNombre, categoriaIcono)

fun CategoriaEntity.aModelo() = Categoria(id, nombre, icono, orden, archivada, usos)

fun Categoria.aEntidad() = CategoriaEntity(id, nombre, icono, orden, archivada, usos)

fun TotalMesEntity.aModelo() = TotalMes(YearMonth.parse(mes), total)

fun IngresoEntity.aModelo() = Ingreso(id, monto, tipo, nota, fecha)

fun IndicadorEntity.aModelo() = Indicador(codigo, nombre, valor, fecha)
