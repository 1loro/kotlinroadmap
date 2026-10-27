package com.example.controlgastos.data.di

import android.content.Context
import com.example.controlgastos.BuildConfig
import com.example.controlgastos.data.local.CategoriaDao
import com.example.controlgastos.data.local.GastoDao
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.IngresoDao
import com.example.controlgastos.data.local.TotalMesDao
import com.example.controlgastos.data.remote.GastosApi
import com.example.controlgastos.data.remote.IndicadoresApi
import com.example.controlgastos.data.remote.crearGastosApi
import com.example.controlgastos.data.remote.crearIndicadoresApi
import com.example.controlgastos.data.repository.CategoriasRepositoryImpl
import com.example.controlgastos.data.repository.GastosRepositoryImpl
import com.example.controlgastos.data.repository.IndicadoresRepositoryImpl
import com.example.controlgastos.data.repository.IngresosRepositoryImpl
import com.example.controlgastos.data.repository.PreferenciasRepositoryImpl
import com.example.controlgastos.data.repository.SesionRepositoryImpl
import com.example.controlgastos.data.seguridad.AlmacenSesion
import com.example.controlgastos.data.seguridad.GestorClaves
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.PreferenciasRepository
import com.example.controlgastos.domain.repository.SesionRepository
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

/** Infraestructura: base de datos y clientes HTTP. Los tests la reemplazan por versiones falsas. */
@Module
@InstallIn(SingletonComponent::class)
object InfraestructuraModule {

    @Provides @Singleton
    fun baseDeDatos(@ApplicationContext context: Context, claves: GestorClaves): GastosDatabase =
        GastosDatabase.construirCifrada(context, claves.claveBaseDeDatos())

    @Provides fun gastoDao(db: GastosDatabase): GastoDao = db.gastoDao()
    @Provides fun categoriaDao(db: GastosDatabase): CategoriaDao = db.categoriaDao()
    @Provides fun totalMesDao(db: GastosDatabase): TotalMesDao = db.totalMesDao()
    @Provides fun ingresoDao(db: GastosDatabase): IngresoDao = db.ingresoDao()
    @Provides fun indicadorDao(db: GastosDatabase): IndicadorDao = db.indicadorDao()

    /** Si el servidor responde 401 (sesión vencida o revocada) se borra la sesión y la app vuelve al login. */
    @Provides @Singleton
    fun gastosApi(almacen: AlmacenSesion): GastosApi = crearGastosApi(
        baseUrl = BuildConfig.GASTOS_API_URL,
        token = { almacen.token },
        sesionExpirada = almacen::borrar,
        registrarPeticiones = BuildConfig.DEBUG,
    )

    @Provides @Singleton
    fun indicadoresApi(): IndicadoresApi =
        crearIndicadoresApi(baseUrl = BuildConfig.INDICADORES_API_URL, registrarPeticiones = BuildConfig.DEBUG)
}

/** "Cuando alguien pida la interfaz X, dale la implementación Y". */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds @Singleton abstract fun sesion(impl: SesionRepositoryImpl): SesionRepository
    @Binds @Singleton abstract fun gastos(impl: GastosRepositoryImpl): GastosRepository
    @Binds @Singleton abstract fun categorias(impl: CategoriasRepositoryImpl): CategoriasRepository
    @Binds @Singleton abstract fun ingresos(impl: IngresosRepositoryImpl): IngresosRepository
    @Binds @Singleton abstract fun indicadores(impl: IndicadoresRepositoryImpl): IndicadoresRepository
    @Binds @Singleton abstract fun preferencias(impl: PreferenciasRepositoryImpl): PreferenciasRepository
}
