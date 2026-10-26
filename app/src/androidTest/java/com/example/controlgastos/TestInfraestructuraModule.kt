package com.example.controlgastos

import android.content.Context
import com.example.controlgastos.data.di.InfraestructuraModule
import com.example.controlgastos.data.local.CotizacionDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.remote.CotizacionDto
import com.example.controlgastos.data.remote.DolarApi
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton

/** API falsa: devuelve lo que diga `respuesta` (o lanza su excepción). */
@Singleton
class FakeDolarApi @Inject constructor() : DolarApi {
    @Volatile var respuesta: () -> List<CotizacionDto> = { emptyList() }
    override suspend fun obtenerDolares() = respuesta()
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
        GastosDatabase.construir(context, enMemoria = true)

    @Provides
    fun movimientoDao(db: GastosDatabase): MovimientoDao = db.movimientoDao()

    @Provides
    fun cotizacionDao(db: GastosDatabase): CotizacionDao = db.cotizacionDao()

    @Provides
    fun dolarApi(fake: FakeDolarApi): DolarApi = fake
}
