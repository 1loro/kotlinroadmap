package com.example.controlgastos

import androidx.fragment.app.FragmentActivity
import com.example.controlgastos.ui.seguridad.Autenticador
import com.example.controlgastos.ui.seguridad.SeguridadModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autenticador falso: el emulador no tiene huella registrada. Por defecto
 * autentica al instante; un test puede hacerlo fallar con `error`.
 */
@Singleton
class FakeAutenticador @Inject constructor() : Autenticador {
    @Volatile var error: String? = null
    @Volatile var intentos = 0
        private set

    override fun disponible(activity: FragmentActivity) = true

    override fun autenticar(activity: FragmentActivity, onExito: () -> Unit, onError: (String) -> Unit) {
        intentos++
        error?.let(onError) ?: onExito()
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SeguridadModule::class])
abstract class TestSeguridadModule {
    @Binds
    abstract fun autenticador(fake: FakeAutenticador): Autenticador
}
