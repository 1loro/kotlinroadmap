package com.example.controlgastos

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Los tests instrumentados arrancan con HiltTestApplication en lugar de
 * GastosApplication: así cada test obtiene un grafo de dependencias nuevo y
 * puede reemplazar módulos (ver TestInfraestructuraModule).
 * Se configura en app/build.gradle.kts (testInstrumentationRunner).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
