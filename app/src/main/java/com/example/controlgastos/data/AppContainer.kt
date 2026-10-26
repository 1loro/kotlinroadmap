package com.example.controlgastos.data

import android.content.Context
import com.example.controlgastos.BuildConfig
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.remote.crearDolarApi

/**
 * Inyección de dependencias MANUAL: un único lugar donde se crean los objetos
 * (BD, cliente HTTP, repositorios) y se conectan entre sí.
 *
 * En la Fase 4 se reemplaza por Hilt, que hace este trabajo automáticamente.
 */
class AppContainer(
    val movimientosRepository: MovimientosRepository,
    val cotizacionesRepository: CotizacionesRepository,
) {
    companion object {
        fun crear(context: Context): AppContainer {
            val db = GastosDatabase.construir(context)
            val api = crearDolarApi(registrarPeticiones = BuildConfig.DEBUG)
            return AppContainer(
                movimientosRepository = MovimientosRepository(db.movimientoDao()),
                cotizacionesRepository = CotizacionesRepository(api, db.cotizacionDao()),
            )
        }
    }
}
