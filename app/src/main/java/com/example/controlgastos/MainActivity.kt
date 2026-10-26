package com.example.controlgastos

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.ui.navegacion.GastosApp
import com.example.controlgastos.ui.seguridad.Autenticador
import com.example.controlgastos.ui.seguridad.BloqueoViewModel
import com.example.controlgastos.ui.seguridad.PantallaBloqueo
import com.example.controlgastos.ui.theme.ControlGastosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "CicloDeVida"

// Punto de entrada de la app. En debug, los métodos on*() registran el ciclo
// de vida en Logcat (etiqueta "CicloDeVida").
//
// FragmentActivity (en lugar de ComponentActivity): la necesita BiometricPrompt.
// @AndroidEntryPoint: Hilt puede inyectar dependencias aquí y en los
// ViewModels de las pantallas que contiene.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var autenticador: Autenticador

    private val bloqueo: BloqueoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate (hay estado guardado: ${savedInstanceState != null})")

        // Release: sin capturas, sin grabación de pantalla y sin miniatura en
        // "Recientes" (ahí se verían los saldos).
        if (BuildConfig.BLOQUEAR_CAPTURAS) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        enableEdgeToEdge()
        setContent {
            ControlGastosTheme {
                val bloqueada by bloqueo.bloqueada.collectAsStateWithLifecycle()
                val error by bloqueo.error.collectAsStateWithLifecycle()

                LaunchedEffect(bloqueada) {
                    if (bloqueada) pedirAutenticacion()
                }

                Box {
                    // La app sigue compuesta debajo (así no pierde la pantalla en
                    // la que estaba), pero sin semántica: TalkBack no puede leer
                    // los saldos mientras está bloqueada.
                    GastosApp(modifier = if (bloqueada) Modifier.clearAndSetSemantics {} else Modifier)
                    if (bloqueada) PantallaBloqueo(error, onDesbloquear = ::pedirAutenticacion)
                }
            }
        }
    }

    private fun pedirAutenticacion() {
        if (!autenticador.disponible(this)) {
            bloqueo.sinMetodoDisponible()
            return
        }
        autenticador.autenticar(this, onExito = bloqueo::desbloqueada, onError = bloqueo::falloAutenticacion)
    }

    override fun onStart() {
        super.onStart()
        log("onStart: visible")
        bloqueo.volvioAPrimerPlano()
    }

    override fun onStop() {
        super.onStop()
        log("onStop: ya no es visible")
        // Una rotación también pasa por onStop: no cuenta como "salir de la app".
        if (!isChangingConfigurations) bloqueo.pasoASegundoPlano()
    }

    override fun onResume() { super.onResume(); log("onResume: en primer plano, interactiva") }
    override fun onPause() { super.onPause(); log("onPause: pierde el foco") }
    override fun onDestroy() { super.onDestroy(); log("onDestroy (isFinishing = $isFinishing)") }

    // Los logs pueden filtrar información: solo en debug (y R8 los elimina de
    // la release, ver keepRules/rules.keep).
    private fun log(mensaje: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, mensaje)
    }
}
