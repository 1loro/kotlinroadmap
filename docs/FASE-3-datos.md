# Fase 3 — Datos, red y persistencia

> **Nota (desde la Fase 4):** `AppContainer` se reemplazó por Hilt, los repositorios se movieron a `data/repository/` (con interfaces en `domain/`) y la lógica de `CotizacionesRuta` pasó a `CotizacionesViewModel`. El código de esta fase se ve con `git show fac07a7`.

Entregable cumplido: la app consume una API pública, muestra los datos y los guarda localmente. Además, los movimientos ahora persisten en una base de datos.

## Arquitectura de datos

```
                 UI (Compose)
                      │  collectAsStateWithLifecycle(Flow)
                      ▼
   MovimientosRepository      CotizacionesRepository
          │                     │                │
          ▼                     ▼                ▼
   MovimientoDao          CotizacionDao      DolarApi (Retrofit)
          └──── Room (SQLite) ────┘          https://dolarapi.com/v1/dolares
```

| Archivo | Qué enseña |
|---|---|
| `data/local/Entidades.kt` | `@Entity`, `@PrimaryKey(autoGenerate)`, `TypeConverter` (LocalDate e Instant ↔ Long), mapeo entidad ↔ modelo |
| `data/local/Daos.kt` | `@Dao`, `@Query` verificado al compilar, `Flow` observable, `suspend`, `@Transaction` |
| `data/local/GastosDatabase.kt` | `@Database`, versión y esquema exportado (`app/schemas/`), callback para sembrar datos |
| `data/remote/DolarApi.kt` | Retrofit + `suspend`, DTO con `@Serializable`, `ignoreUnknownKeys`, timeouts, log HTTP solo en debug |
| `data/Repositorios.kt` | patrón Repository, estrategia **offline-first**, errores tipados con `sealed class` |
| `data/AppContainer.kt` | inyección de dependencias **manual** (en la Fase 4 la reemplaza Hilt) |
| `ui/cotizaciones/CotizacionesUiState.kt` | estados **carga / éxito / error** modelados con `sealed interface` |
| `ui/cotizaciones/CotizacionesScreen.kt` | `LaunchedEffect`, `PullToRefreshBox`, separación Ruta (con efectos) / Screen (sin estado) |

## Por qué DolarApi y no Frankfurter

Frankfurter usa las cotizaciones del Banco Central Europeo, que **no incluye pesos argentinos** (`/v1/currencies` no tiene ARS). DolarApi es gratis, no pide clave y da el dólar oficial, blue, MEP, CCL, etc. Es más relevante para una app con dominio bancario argentino.

## Ideas clave

**Offline-first.** La UI *nunca* lee directo de la red. Siempre lee de Room, y la red solo *actualiza* Room:
- Con conexión, los datos llegan, se guardan y Room avisa a la UI por el `Flow`.
- Sin conexión, la UI muestra lo último guardado con un aviso ("Mostrando datos guardados del…").
- Sin conexión y sin nada guardado, se muestra el estado de error con "Reintentar".

**Entidad ≠ DTO ≠ modelo.** Hay tres clases para una cotización:
- `CotizacionDto` es el formato de la API.
- `CotizacionEntity` es la tabla de Room.
- `Cotizacion` es lo que usa la app.

Si la API cambia un campo, solo cambia el DTO y su mapeo.

**No usar `runCatching` con corrutinas.** Atrapa también `CancellationException` y rompe la cancelación. En `CotizacionesRepository.actualizar()` se atrapan solo las excepciones esperables (`IOException`, `HttpException`, `SerializationException`).

**Migraciones.** Si agregas un campo a una entidad, tienes que subir `version` en `@Database` y escribir una `Migration`. Si no, la app falla al abrir la BD de los usuarios existentes. El JSON de `app/schemas/` (versionado en Git) es la referencia de cada versión.

## Lección de depuración: un test intermitente que destapó un problema de diseño

El test de UI `agregarVerYEliminarUnGasto` fallaba **a veces**: casi siempre con la app recién instalada, casi nunca en las corridas siguientes. Subir el timeout no lo arreglaba. Esto es lo que se midió:

1. Se volcó el árbol de la UI al fallar: el Inicio mostraba `$ 0,00`.
2. Una sonda leyó la BD desde el test: **tenía las 9 filas**.
3. Un "DAO espía" mostró que el `Flow` **sí emitía** las 9 filas.
4. Un log en `GastosApp` mostró que la pantalla **sí se recomponía** con los 9 datos… pero lo visible seguía vacío.

**Causa:** `GastosApp` leía los `Flow` *arriba* del `NavHost` y pasaba los valores por las lambdas de cada ruta. Cada emisión cambiaba el grafo de navegación entero. Con la app recién instalada (más lenta), el dato llegaba justo durante la transición inicial y lo visible quedaba desactualizado.

**Solución:** cada destino lee *sus* datos dentro de su propio `composable<...> { }`. Es además el diseño correcto, el mismo que tendrán los ViewModel en la Fase 4. Después del cambio, el test pasó 4 de 4 veces con instalación nueva.

Moraleja: un test intermitente no se "arregla" subiendo el timeout. Hay que medir hasta encontrar la causa.

## Tests

| Comando | Qué prueba |
|---|---|
| `./gradlew :app:testDebugUnitTest` | `LogicaTest` (incluye los estados carga/éxito/error) y `CotizacionesRepositoryTest`: Retrofit real contra **MockWebServer** (JSON correcto, HTTP 503, JSON inválido, servidor caído) |
| `./gradlew :app:connectedDebugAndroidTest` | `DaosTest`: Room con SQLite real en memoria. `NavegacionTest`: flujos completos con BD en memoria y API falsa, incluido el estado sin conexión |

## Práctica (hazlo tú)

1. **Comprueba la persistencia:** agrega un gasto, cierra la app desde *Recientes* y vuelve a abrirla.
2. **Comprueba el modo sin conexión:** activa el modo avión, abre "Dólar" y mira el aviso. Desactívalo y desliza hacia abajo.
3. **Mira la red:** en Logcat filtra por `okhttp` y verás `--> GET https://dolarapi.com/v1/dolares`.
4. **Explora la BD:** en Android Studio abre *App Inspection → Database Inspector* y ejecuta `SELECT categoria, SUM(monto) FROM movimientos GROUP BY categoria`.
5. **Migración (desafío):** agrega `val nota: String? = null` a `MovimientoEntity`, sube a `version = 2` y escribe la `Migration(1, 2)` con `ALTER TABLE movimientos ADD COLUMN nota TEXT`.
6. **Nueva consulta:** agrega al DAO `@Query("SELECT * FROM movimientos WHERE descripcion LIKE '%' || :texto || '%'")` y un buscador en la pantalla Movimientos.
