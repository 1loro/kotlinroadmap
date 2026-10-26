# Fase 4 — Arquitectura y buenas prácticas

Entregable cumplido: refactor de la app de la Fase 3 con **MVVM + capas + Hilt + pruebas unitarias**.

## Capas

```
┌──────────────────────── ui ────────────────────────┐
│  InicioRuta ─► InicioScreen(estado)                │
│      │              ▲                              │
│      ▼              │ StateFlow<InicioUiState>     │
│  InicioViewModel ───┘    (uno por pantalla)        │
└──────┬─────────────────────────────────────────────┘
       │ usa
┌──────▼──────────────── domain (Kotlin puro) ───────┐
│  Casos de uso: AgregarMovimientoUseCase,           │
│                ObtenerPanelInicioUseCase           │
│  Interfaces:   MovimientosRepository,              │
│                CotizacionesRepository              │
│  Modelos:      Movimiento, Cotizacion, Resumen     │
└──────▲─────────────────────────────────────────────┘
       │ implementa
┌──────┴──────────────── data ───────────────────────┐
│  MovimientosRepositoryImpl ─► Room                 │
│  CotizacionesRepositoryImpl ─► Retrofit + Room     │
│  di/DataModule.kt: le dice a Hilt cómo crearlos    │
└────────────────────────────────────────────────────┘
```

**Regla de dependencias:** `ui` → `domain` ← `data`. El dominio no conoce a nadie: no importa Room, Retrofit ni Android. Por eso sus reglas y los ViewModels se testean en la JVM con repositorios falsos, en milisegundos.

## Qué cambió respecto de la Fase 3

| Antes (Fase 3) | Ahora (Fase 4) | Por qué |
|---|---|---|
| Estado en composables (`remember`, `rememberSaveable`) | Estado en **ViewModels** con `StateFlow` | Sobrevive a la rotación, se testea sin UI |
| `LaunchedEffect` en `CotizacionesRuta` pedía a la API | `CotizacionesViewModel.actualizar()` | La lógica sale de la UI |
| Validación del formulario dentro de `FormularioScreen` | `AgregarMovimientoUseCase` en el dominio | Regla de negocio reutilizable y testeable |
| Conversión a dólares calculada en la UI | `ObtenerPanelInicioUseCase` | Combina dos fuentes (`combine`) en un solo lugar |
| `AppContainer` (DI manual) | **Hilt** (`@HiltAndroidApp`, `@HiltViewModel`, `@Module`) | Menos código de "cableado", alcance de cada objeto declarado |
| Repositorios como clases concretas | **Interfaces** en domain + `Impl` en data | Los tests usan implementaciones falsas |
| `LocalDate.now()` en el código | `Clock` inyectado | Los tests usan una fecha fija |

## Piezas de MVVM en el código

| Concepto | Dónde verlo |
|---|---|
| `UiState` como `data class` | `FormularioUiState`, `MovimientosUiState` |
| `UiState` como `sealed interface` | `InicioUiState`, `DetalleUiState`, `CotizacionesUiState` |
| `MutableStateFlow` privado + `StateFlow` público | `FormularioViewModel._uiState` / `uiState` |
| `stateIn(WhileSubscribed(5_000))` | Todos los ViewModels que leen de la BD |
| `SavedStateHandle`: argumentos de navegación | `DetalleViewModel` lee `"id"` de la ruta `Detalle(id)` |
| `SavedStateHandle`: sobrevivir a la muerte del proceso | `MovimientosViewModel` guarda el filtro |
| Eventos de una sola vez modelados como estado | `FormularioUiState.guardado`, `DetalleViewModel.eliminado` |
| Ruta (con ViewModel) / Screen (sin estado) | Cada `*Screen.kt` tiene `XRuta` y `XScreen` |

## Hilt en 5 anotaciones

| Anotación | Dónde | Qué hace |
|---|---|---|
| `@HiltAndroidApp` | `GastosApplication` | Crea el contenedor de dependencias de la app |
| `@AndroidEntryPoint` | `MainActivity` | Permite inyectar en la Activity y en sus ViewModels |
| `@HiltViewModel` + `@Inject constructor` | ViewModels | Hilt crea el ViewModel con sus dependencias; en Compose: `hiltViewModel()` |
| `@Inject constructor` | Repositorios, casos de uso | Hilt sabe construir la clase |
| `@Module` + `@Provides` / `@Binds` | `data/di/DataModule.kt` | Enseña a crear lo que no es nuestro (Room, Retrofit) o a qué implementación corresponde cada interfaz |

En los tests instrumentados, `@TestInstallIn` reemplaza `InfraestructuraModule` por `TestInfraestructuraModule`, con BD en memoria y API falsa, sin tocar ni una línea de la app.

## Tests

| Tipo | Archivo | Qué prueba |
|---|---|---|
| Unitario (dominio) | `domain/CasosDeUsoTest.kt` | Validaciones, guardado normalizado, saldo en dólares con y sin cotización |
| Unitario (ViewModel) | `ui/ViewModelsTest.kt` | Los 5 ViewModels con repositorios falsos (`fakes/Fakes.kt`) |
| Unitario (data) | `data/repository/CotizacionesRepositoryImplTest.kt` | Retrofit real contra MockWebServer |
| Instrumentado | `data/DaosTest.kt` | Room con SQLite real |
| Instrumentado (UI) | `NavegacionTest.kt` | La app completa con Hilt: agregar, filtrar, cotizaciones, sin conexión |

Técnicas que aparecen en los tests:
- **`MainDispatcherRule`**: reemplaza `Dispatchers.Main`, que no existe en la JVM.
- **`observar(uiState)`**: con `WhileSubscribed`, un `StateFlow` solo calcula si alguien lo observa; en el test, un colector en `backgroundScope` hace de "pantalla".
- **Turbine** (`uiState.test { awaitItem() }`): verifica la *secuencia* de estados (Cargando → Éxito).
- **`CompletableDeferred`** en el repositorio falso simula una red lenta y controlable.
- **`Clock.fixed(...)`**: fecha predecible.

Totales: 29 tests unitarios y 7 instrumentados.

## Práctica (hazlo tú)

1. **Rota el emulador** con el formulario a medio llenar: los datos se mantienen. Compáralo con el experimento de la Fase 2.
2. **Muerte del proceso:** activa *Opciones de desarrollador → No conservar actividades*, elige el filtro "Gastos", sal con Home y vuelve. El filtro se conserva (SavedStateHandle), pero el texto del formulario no (solo vive en el ViewModel). ¿Cómo lo guardarías? Pista: `savedStateHandle.getStateFlow`.
3. **Nuevo caso de uso:** crea `EliminarMovimientoUseCase` que impida borrar ingresos de más de 30 días, con su test usando `Clock.fixed`.
4. **Test que falle primero (TDD):** escribe en `ViewModelsTest` un test para "el formulario recorta espacios del monto" y luego modifica el código hasta que pase.
5. **Módulo Gradle (desafío):** mueve `domain/` a un módulo `:domain` de Kotlin puro (como `:fase0-kotlin`). El compilador impedirá entonces que el dominio importe Android por error.
