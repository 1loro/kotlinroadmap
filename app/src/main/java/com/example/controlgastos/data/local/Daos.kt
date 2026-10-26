package com.example.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// DAO = Data Access Object. Room genera la implementación a partir de las
// anotaciones y valida el SQL al compilar (un error de tipeo no compila).
//
// - Las funciones que devuelven Flow son "observables": emiten de nuevo
//   cada vez que la tabla cambia. La UI se actualiza sola.
// - Las funciones suspend se ejecutan fuera del hilo principal.

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE id = :id")
    fun observarPorId(id: Long): Flow<MovimientoEntity?>

    @Insert
    suspend fun insertar(movimiento: MovimientoEntity): Long

    @Query("DELETE FROM movimientos WHERE id = :id")
    suspend fun eliminar(id: Long)
}

@Dao
interface CotizacionDao {
    @Query("SELECT * FROM cotizaciones ORDER BY orden")
    fun observarTodas(): Flow<List<CotizacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(cotizaciones: List<CotizacionEntity>)

    @Query("DELETE FROM cotizaciones")
    suspend fun borrarTodas()

    // @Transaction: o se ejecutan ambas operaciones, o ninguna. La UI nunca
    // ve la tabla vacía a mitad del reemplazo.
    @Transaction
    suspend fun reemplazarTodas(cotizaciones: List<CotizacionEntity>) {
        borrarTodas()
        insertarTodas(cotizaciones)
    }
}
