package com.example.controlgastos

import android.content.Context
import com.example.controlgastos.data.di.InfraestructuraModule
import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.remote.IndicadorDto
import com.example.controlgastos.data.remote.IndicadoresApi
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton

/** API falsa: devuelve lo que diga `respuesta` (o lanza su excepción). */
@Singleton
class FakeIndicadoresApi @Inject constructor() : IndicadoresApi {
    @Volatile var respuesta: () -> List<IndicadorDto> = { emptyList() }
    override suspend fun obtenerIndicadores() = respuesta()
}

/**
 * En TODOS los tests instrumentados, Hilt usa este módulo en lugar de
 * InfraestructuraModule: BD en memoria (nueva en cada test) y API falsa
 * (sin red). El resto de la app (repositorios, casos de uso, ViewModels) es
 * el código REAL.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [InfraestructuraModule::class])
object TestInfraestructuraModule {

    @Provides @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): GastosDatabase =
        GastosDatabase.construirEnMemoria(context)

    @Provides
    fun movimientoDao(db: GastosDatabase): MovimientoDao = db.movimientoDao()

    @Provides
    fun indicadorDao(db: GastosDatabase): IndicadorDao = db.indicadorDao()

    @Provides
    fun dolarApi(fake: FakeIndicadoresApi): IndicadoresApi = fake
}
