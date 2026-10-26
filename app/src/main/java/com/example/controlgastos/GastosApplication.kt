package com.example.controlgastos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp genera el "contenedor" de dependencias de toda la app.
// La clase Application se crea antes que cualquier Activity y vive mientras
// viva el proceso.
@HiltAndroidApp
class GastosApplication : Application()
