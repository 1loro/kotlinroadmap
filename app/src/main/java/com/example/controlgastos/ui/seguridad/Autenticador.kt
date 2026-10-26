package com.example.controlgastos.ui.seguridad

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Pide al usuario que confirme su identidad. Es una interfaz para que los
 * tests de UI puedan reemplazarla (un emulador no tiene huella registrada).
 */
interface Autenticador {
    /** false si el dispositivo no tiene huella, rostro ni bloqueo de pantalla configurado. */
    fun disponible(activity: FragmentActivity): Boolean

    fun autenticar(activity: FragmentActivity, onExito: () -> Unit, onError: (String) -> Unit)
}

class AutenticadorBiometrico @Inject constructor() : Autenticador {

    // Biometría o, como alternativa, el PIN/patrón/contraseña del dispositivo.
    // Android 9-10 no permite combinar biometría "fuerte" con la credencial.
    private val permitidos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    } else {
        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }

    override fun disponible(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(permitidos) == BiometricManager.BIOMETRIC_SUCCESS

    override fun autenticar(activity: FragmentActivity, onExito: () -> Unit, onError: (String) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onExito()

            // Errores definitivos (canceló, demasiados intentos...). Un dedo no
            // reconocido NO llega aquí: el diálogo deja reintentar solo.
            override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) = onError(mensaje.toString())
        }
        val datos = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Control de Gastos")
            .setSubtitle("Confirma que eres tú para ver tus finanzas")
            .setAllowedAuthenticators(permitidos)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(datos)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SeguridadModule {
    @Binds
    abstract fun autenticador(impl: AutenticadorBiometrico): Autenticador
}
