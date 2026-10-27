package com.example.controlgastos.data.di

import android.content.Context
import com.example.controlgastos.BuildConfig
import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.remote.IndicadoresApi
import com.example.controlgastos.data.remote.crearIndicadoresApi
import com.example.controlgastos.data.repository.IndicadoresRepositoryImpl
import com.example.controlgastos.data.repository.MovimientosRepositoryImpl
import com.example.controlgastos.data.seguridad.GestorClaves
import com.example.controlgastos.domain.repository.IndicadoresRepository
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
    fun baseDeDatos(@ApplicationContext context: Context, claves: GestorClaves): GastosDatabase =
        GastosDatabase.construirCifrada(context, claves.claveBaseDeDatos())

    @Provides
    fun movimientoDao(db: GastosDatabase): MovimientoDao = db.movimientoDao()

    @Provides
    fun indicadorDao(db: GastosDatabase): IndicadorDao = db.indicadorDao()

    @Provides @Singleton
    fun dolarApi(): IndicadoresApi =
        crearIndicadoresApi(baseUrl = BuildConfig.INDICADORES_API_URL, registrarPeticiones = BuildConfig.DEBUG)
}

/** "Cuando alguien pida la interfaz X, dale la implementación Y". */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {

    @Binds @Singleton
    abstract fun movimientos(impl: MovimientosRepositoryImpl): MovimientosRepository

    @Binds @Singleton
    abstract fun indicadores(impl: IndicadoresRepositoryImpl): IndicadoresRepository
}
