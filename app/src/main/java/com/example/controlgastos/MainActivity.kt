package com.example.controlgastos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.controlgastos.ui.theme.ControlGastosTheme

private const val TAG = "CicloDeVida"

// Fase 1: punto de entrada de la app. Los métodos on*() registran el ciclo
// de vida de la Activity en Logcat (filtrar por la etiqueta "CicloDeVida").
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate (savedInstanceState = $savedInstanceState)")
        enableEdgeToEdge()
        setContent {
            ControlGastosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Saludo("Mundo", Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart: visible") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume: en primer plano, interactiva") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause: pierde el foco") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop: ya no es visible") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy (isFinishing = $isFinishing)") }
}

@Composable
fun Saludo(nombre: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("¡Hola $nombre!", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun SaludoPreview() {
    ControlGastosTheme { Saludo("Mundo") }
}
