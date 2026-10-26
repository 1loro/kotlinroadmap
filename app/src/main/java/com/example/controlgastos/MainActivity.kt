package com.example.controlgastos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.controlgastos.ui.navegacion.GastosApp
import com.example.controlgastos.ui.theme.ControlGastosTheme

private const val TAG = "CicloDeVida"

// Punto de entrada de la app. Los métodos on*() registran el ciclo de vida
// de la Activity en Logcat (filtrar por la etiqueta "CicloDeVida").
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate (savedInstanceState = $savedInstanceState)")
        enableEdgeToEdge()
        val container = (application as GastosApplication).container
        setContent {
            ControlGastosTheme {
                GastosApp(container)
            }
        }
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart: visible") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume: en primer plano, interactiva") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause: pierde el foco") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop: ya no es visible") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy (isFinishing = $isFinishing)") }
}
