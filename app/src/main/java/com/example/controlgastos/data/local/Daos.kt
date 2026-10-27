package com.example.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// DAO = Data Access Object. Room genera la implementación a partir de las
// anotaciones y valida el SQL al compilar (un error de tipeo no compila).
//
// - Las funciones que devuelven Flow son "observables": emiten de nuevo
//   cada vez que la tabla cambia. La UI se actualiza sola.
// - Las funciones suspend se ejecutan fuera del hilo principal.
// - @Transaction: o se ejecuta todo, o nada. La UI nunca ve una tabla a medio
//   reemplazar.

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos WHERE mes = :mes ORDER BY fecha DESC, id DESC")
    fun observarMes(mes: String): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos ORDER BY fecha DESC, id DESC LIMIT :cantidad")
    fun observarRecientes(cantidad: Int): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE id = :id")
    fun observarPorId(id: Long): Flow<GastoEntity?>

    @Upsert
    suspend fun guardar(gasto: GastoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(gastos: List<GastoEntity>)

    @Query("DELETE FROM gastos WHERE mes = :mes")
    suspend fun borrarMes(mes: String)

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM gastos")
    suspend fun borrarTodos()

    /** Reemplaza la caché de un mes con lo que devolvió el servidor. */
    @Transaction
    suspend fun reemplazarMes(mes: String, gastos: List<GastoEntity>) {
        borrarMes(mes)
        insertarTodos(gastos)
    }
}

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY orden, id")
    fun observarTodas(): Flow<List<CategoriaEntity>>

    @Upsert
    suspend fun guardar(categoria: CategoriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(categorias: List<CategoriaEntity>)

    @Query("DELETE FROM categorias WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM categorias")
    suspend fun borrarTodas()

    @Transaction
    suspend fun reemplazarTodas(categorias: List<CategoriaEntity>) {
        borrarTodas()
        insertarTodas(categorias)
    }
}

@Dao
interface TotalMesDao {
    @Query("SELECT * FROM totales_mes WHERE mes BETWEEN :desde AND :hasta ORDER BY mes")
    fun observarEntre(desde: String, hasta: String): Flow<List<TotalMesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(totales: List<TotalMesEntity>)

    @Query("DELETE FROM totales_mes")
    suspend fun borrarTodos()
}

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingresos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, id DESC")
    fun observarEntre(desde: Long, hasta: Long): Flow<List<IngresoEntity>>

    @Insert
    suspend fun insertar(ingreso: IngresoEntity): Long

    @Query("DELETE FROM ingresos WHERE id = :id")
    suspend fun borrar(id: Long)
}

@Dao
interface IndicadorDao {
    @Query("SELECT * FROM indicadores ORDER BY orden")
    fun observarTodas(): Flow<List<IndicadorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(indicadores: List<IndicadorEntity>)

    @Query("DELETE FROM indicadores")
    suspend fun borrarTodas()

    @Transaction
    suspend fun reemplazarTodas(indicadores: List<IndicadorEntity>) {
        borrarTodas()
        insertarTodas(indicadores)
    }
}
