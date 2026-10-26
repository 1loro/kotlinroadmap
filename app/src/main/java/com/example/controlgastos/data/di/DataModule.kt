package com.example.controlgastos.data.di

import android.content.Context
import com.example.controlgastos.BuildConfig
import com.example.controlgastos.data.local.CotizacionDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.remote.DolarApi
import com.example.controlgastos.data.remote.crearDolarApi
import com.example.controlgastos.data.repository.CotizacionesRepositoryImpl
import com.example.controlgastos.data.repository.MovimientosRepositoryImpl
import com.example.controlgastos.domain.repository.CotizacionesRepository
import com.example.controlgastos.domain.repository.MovimientosRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Módulos de Hilt: le enseñan a crear lo que NO puede construir solo
// (clases de librerías, interfaces). Reemplazan al AppContainer de la Fase 3.
//
// @InstallIn(SingletonComponent::class): viven mientras viva la app.
// @Singleton: una sola instancia compartida (una BD, un cliente HTTP).

/** Infraestructura: base de datos y cliente HTTP. Los tests la reemplazan por versiones falsas. */
@Module
@InstallIn(SingletonComponent::class)
object InfraestructuraModule {

    @Provides @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): GastosDatabase = GastosDatabase.construir(context)

    @Provides
    fun movimientoDao(db: GastosDatabase): MovimientoDao = db.movimientoDao()

    @Provides
    fun cotizacionDao(db: GastosDatabase): CotizacionDao = db.cotizacionDao()

    @Provides @Singleton
    fun dolarApi(): DolarApi = crearDolarApi(registrarPeticiones = BuildConfig.DEBUG)
}

/** "Cuando alguien pida la interfaz X, dale la implementación Y". */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {

    @Binds @Singleton
    abstract fun movimientos(impl: MovimientosRepositoryImpl): MovimientosRepository

    @Binds @Singleton
    abstract fun cotizaciones(impl: CotizacionesRepositoryImpl): CotizacionesRepository
}
