package com.example.controlgastos

import android.app.Application
import com.example.controlgastos.data.AppContainer

// La clase Application se crea antes que cualquier Activity y vive mientras
// viva el proceso: es el lugar adecuado para objetos únicos como la BD.
class GastosApplication : Application() {
    val container: AppContainer by lazy { AppContainer.crear(this) }
}
