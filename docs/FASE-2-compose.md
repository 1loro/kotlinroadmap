# Fase 2 — UI con Jetpack Compose

Entregable cumplido: app multipantalla con navegación y lista interactiva, sin datos externos (todo en memoria).

## Pantallas y flujo

```
            ┌──────────── barra inferior ────────────┐
            │                                         │
        [Inicio] ──"Ver todos"──▶ [Movimientos]       │
         │  │                       │   │             │
   FAB "Nuevo"  toca ítem      toca ítem  FAB "+"
         │  │                       │   │
         │  └──────▶ [Detalle(id)] ◀┘   │
         │              │ eliminar      │
         └──▶ [NuevoMovimiento] ◀───────┘
                  │ guardar → vuelve atrás
```

| Archivo | Qué enseña |
|---|---|
| `ui/navegacion/GastosApp.kt` | `NavHost`, rutas type-safe con `@Serializable`, argumentos (`Detalle(id)`), `NavigationBar`, patrón de pestañas (`popUpTo` + `saveState`) |
| `ui/inicio/InicioScreen.kt` | `Scaffold`, `TopAppBar`, `Card`, `LazyColumn` con secciones (`item` / `items`), `LinearProgressIndicator` |
| `ui/movimientos/MovimientosScreen.kt` | `FilterChip`, `rememberSaveable`, lista con `key` y `animateItem()`, estado vacío |
| `ui/detalle/DetalleScreen.kt` | `ListItem`, `AlertDialog` de confirmación, manejo de un argumento inexistente |
| `ui/formulario/FormularioScreen.kt` | `OutlinedTextField`, teclado numérico, validación, `SegmentedButton`, `FlowRow` |
| `ui/componentes/MovimientoItem.kt` | componente reutilizable, `Modifier` como parámetro, `@Preview` |
| `ui/GastosState.kt` | `mutableStateListOf`, *state holder*, elevación de estado (*state hoisting*) |
| `ui/theme/Theme.kt` | `MaterialTheme`, roles de color de Material 3, modo oscuro, dynamic color |

## Ideas clave

**UI declarativa.** No se dice "cambia el texto de este TextView". Se describe la UI *en función del estado*: `Text(formatearMonto(resumen.saldo))`. Cuando el estado cambia, Compose vuelve a ejecutar esas funciones, lo que se llama **recomposición**.

**`remember` vs `rememberSaveable`.**
- `remember { mutableStateOf(x) }` sobrevive a las recomposiciones, pero **no** a la rotación.
- `rememberSaveable` también sobrevive a la rotación, porque guarda en el `Bundle`. Sirve para valores simples (texto de un campo, filtro elegido).
- Prueba esto: escribe algo en el formulario y rota el emulador. El texto se mantiene. Ahora agrega un movimiento, vuelve al Inicio y rota: **el movimiento desaparece**, porque `GastosState` usa `remember`. Esa limitación es intencional y se resuelve en la Fase 4 con `ViewModel`.

**State hoisting (elevación de estado).** Las pantallas no guardan los datos: los reciben como parámetros (`movimientos`) y avisan de los eventos con lambdas (`onMovimientoClick`). Así se pueden previsualizar y testear con datos falsos. Los `@Preview` lo demuestran.

**Modifier.** Su orden importa: `.clickable().padding(16.dp)` hace clicable también el área del padding, y `.padding().clickable()` no.

**Rutas type-safe.** `navController.navigate(Detalle(id = 5))`. Si te olvidas del `id`, no compila. Antes las rutas eran strings como `"detalle/{id}"`, propensas a errores.

## Tests

| Comando | Qué prueba |
|---|---|
| `./gradlew :app:testDebugUnitTest` | `LogicaTest`: resumen, parseo de montos, estado (JVM, rápido) |
| `./gradlew :app:connectedDebugAndroidTest` | `NavegacionTest`: flujo completo en el emulador (agregar, ver, eliminar, filtrar) |

## Práctica (hazlo tú)

1. **Fecha elegible:** hoy todo movimiento se guarda con `LocalDate.now()`. Agrega un campo que abra `DatePickerDialog` de Material 3.
2. **Deslizar para borrar:** en `MovimientosScreen`, envuelve cada ítem en `SwipeToDismissBox`.
3. **Nueva categoría:** agrega `EDUCACION("Educación", "📚", GASTO)` al enum. ¿Cuántos archivos tuviste que tocar? (Respuesta: uno, gracias a `Categoria.de(tipo)`.)
4. **Pantalla de edición:** reutiliza `FormularioScreen` para editar un movimiento existente (pista: una ruta `EditarMovimiento(id)` y valores iniciales).
5. Cambia `dynamicColor = true` en `Theme.kt` y cambia el fondo de pantalla del emulador.
