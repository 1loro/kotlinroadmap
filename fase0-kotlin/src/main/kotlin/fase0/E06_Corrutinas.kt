package fase0

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =====================================================================
// Tema: corrutinas — suspend, launch y async.
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 15 — Consultar saldos de varias cuentas en paralelo
//
// - `suspend` marca una función que puede pausarse sin bloquear el hilo
//   (por ejemplo, mientras espera una respuesta de red).
// - `launch` inicia una tarea "dispara y olvida" (no devuelve valor).
// - `async` inicia una tarea que devuelve un resultado (Deferred) que se
//   obtiene con await().
// - `coroutineScope` espera a que terminen todas las corrutinas hijas
//   (concurrencia estructurada).
// ---------------------------------------------------------------------

// Simula una llamada de red lenta.
suspend fun consultarSaldo(cuenta: String, demoraMs: Long): Double {
    delay(demoraMs)  // a diferencia de Thread.sleep, no bloquea el hilo
    return cuenta.length * 1000.0
}

// Secuencial: el tiempo total es la SUMA de las demoras.
suspend fun saldoTotalSecuencial(cuentas: Map<String, Long>): Double {
    var total = 0.0
    for ((cuenta, demora) in cuentas) total += consultarSaldo(cuenta, demora)
    return total
}

// Paralelo: el tiempo total es aproximadamente la demora MÁS LARGA.
suspend fun saldoTotalParalelo(cuentas: Map<String, Long>): Double = coroutineScope {
    cuentas
        .map { (cuenta, demora) -> async { consultarSaldo(cuenta, demora) } }
        .awaitAll()
        .sum()
}

// launch: tareas sin resultado; coroutineScope espera a todas.
suspend fun notificarCuentas(cuentas: List<String>, registro: MutableList<String>) = coroutineScope {
    cuentas.forEach { cuenta ->
        launch {
            delay(10)
            synchronized(registro) { registro.add("Notificada: $cuenta") }
        }
    }
}
