# Fase 0 — Kotlin desde cero

Código: `fase0-kotlin/` (módulo de Kotlin puro, sin Android).

| Comando | Qué hace |
|---|---|
| `./gradlew :fase0-kotlin:run` | Ejecuta la demo de los 15 ejercicios |
| `./gradlew :fase0-kotlin:test` | Corre los tests (`EjerciciosTest.kt`) |

En Android Studio también puedes pulsar el ▶ verde junto a `fun main()` en `Main.kt`.

## Mapa de ejercicios

| # | Ejercicio | Archivo | Conceptos |
|---|---|---|---|
| 1 | Calculadora | `E01_Basicos.kt` | `enum`, `when`, `companion object`, retorno `Double?` |
| 2 | FizzBuzz | `E01_Basicos.kt` | `when` sin argumento |
| 3 | Palíndromo | `E01_Basicos.kt` | funciones de `String`, `filter` |
| 4 | Factorial / Fibonacci / primos | `E01_Basicos.kt` | `val` vs `var`, `tailrec`, `repeat`, `while`, rangos, referencia `::esPrimo` |
| 5 | Parseo seguro de montos | `E02_NullSafety.kt` | `?.`, `?:`, `!!`, `toDoubleOrNull`, `takeIf` |
| 6 | Estadísticas | `E02_NullSafety.kt` | `data class`, resultado anulable |
| 7 | Orden superior | `E03_Funciones.kt` | tipos función `(Int) -> Int`, genéricos, closures |
| 8 | Extensiones | `E03_Funciones.kt` | funciones de extensión, parámetros por defecto |
| 9 | Figuras | `E04_Clases.kt` | `interface`, `abstract`, `open`, `override`, herencia |
| 10 | Carrito | `E04_Clases.kt` | `data class`, `copy()`, lista privada mutable / pública inmutable |
| 11 | Generador de IDs | `E04_Clases.kt` | `object` (singleton), constructor privado + fábrica |
| 12 | Frecuencia de palabras | `E05_Colecciones.kt` | `Map`, `groupingBy`, `eachCount`, `sortedWith` |
| 13 | Comparar cursos | `E05_Colecciones.kt` | `Set`, `intersect`, `subtract`, `union` |
| 14 | Resumen de gastos | `E05_Colecciones.kt` | `partition`, `groupBy`, `sumOf`, `maxByOrNull`, desestructuración |
| 15 | Saldos en paralelo | `E06_Corrutinas.kt` | `suspend`, `launch`, `async/await`, `coroutineScope`, `runTest` |

## Ideas clave

- **`val` por defecto, `var` solo si el valor cambia.** La inmutabilidad evita errores.
- **Null safety:** `String` nunca es null; `String?` puede serlo. El compilador te obliga a manejar el caso. Usa `?.` y `?:`, y evita `!!`.
- **`data class`** genera automáticamente `equals`, `toString` y `copy`. Es ideal para modelos como `Movimiento`.
- **Corrutinas:** `delay()` *suspende* (libera el hilo) y `Thread.sleep()` *bloquea*. En Android esto es crítico: si bloqueas el hilo principal, la app se congela.

## Práctica (hazlo tú)

1. Agrega `Operacion.POTENCIA("^")` a la calculadora y un test. Pista: `Math.pow` o `a.pow(b)`.
2. Crea `class Triangulo(base, altura, ladoA, ladoB, ladoC) : Figura` y prueba `areaTotal`.
3. En el ejercicio 14, agrega a `ResumenGastos` un campo `porcentajePorCategoria: Map<String, Double>`.
4. Rompe un test a propósito (cambia un valor esperado), mira el mensaje de error y arréglalo.
5. En el ejercicio 15, reemplaza `delay` por `Thread.sleep` y observa qué pasa con el tiempo paralelo en `main`. ¿Por qué?
