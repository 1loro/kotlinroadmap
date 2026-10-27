# gastos. en Android: la misma app que la PWA del iPhone

La app Android pasó a ser **gastos.**, la versión nativa de la PWA [gastos-pi-five.vercel.app](https://gastos-pi-five.vercel.app). Tiene el mismo diseño, las mismas pantallas y **la misma cuenta**: lo que registras en el iPhone aparece aquí, y al revés.

## Cómo se conecta con la web

```
iPhone (PWA)  ──cookie──┐
Atajo de iPhone ─token──┤
                        ├──►  Next.js en Vercel (src/app/api)  ──►  Postgres (Neon)
Android ──Bearer token──┘        /api/app/*  (nuevo)
```

Al backend de la web (repositorio `finance`) se le agregó una **API JSON para la app** bajo `/api/app/*`, sin tocar las rutas del Atajo:

| Ruta | Para qué |
|---|---|
| `POST /api/app/auth/login`, `/registro` | Devuelven una sesión firmada (HMAC, 60 días), la misma que la cookie de la web con "Recuérdame" |
| `GET/PATCH /api/app/yo` | Perfil: nombre, moneda y zona horaria |
| `GET/POST /api/app/gastos`, `PATCH/DELETE /api/app/gastos/{id}` | Gastos de un mes y su edición |
| `GET/POST /api/app/categorias`, `PATCH/DELETE /api/app/categorias/{id}` | Categorías (si tienen gastos, se archivan en vez de borrarse) |
| `GET /api/app/totales` | Total por mes, para las barras |

No hizo falta cambiar la base de datos. Las validaciones son las mismas que usa la web (`lib/validation.ts`).

## Qué hay en la app

| Pantalla | Equivale a | Extras de Android |
|---|---|---|
| Login / Registro | `/login`, `/registro` | La sesión se guarda cifrada (Keystore) |
| Inicio | `/` | Balance con ingresos e indicadores de Chile |
| Movimientos | `/movimientos` | Ingresos del mes |
| Nuevo / Editar gasto | `/nuevo`, `/movimientos/[id]` | Selector Gasto / Ingreso |
| Comparar | `/comparar` | — |
| Ajustes | `/ajustes` | Seguridad (en lugar del Atajo de iPhone) |
| Indicadores | — | Dólar, euro, UF y UTM (mindicador.cl) |

**Ingresos:** el backend solo registra gastos, así que los ingresos se guardan **solo en el teléfono** (Room cifrado). La migración de la BD 2→3 conservó los ingresos que ya existían.

## Del diseño web a Compose

| Web (Tailwind) | Android (Compose) |
|---|---|
| Variables de `globals.css` (`--canvas`, `--accent`…) | `Colores` en `ui/theme/Theme.kt`, claro y oscuro |
| Geist (`next/font`) | `res/font/geist_*.ttf` |
| Íconos `lucide-react` | 57 vector drawables `lucide_*`, generados desde el paquete (licencia ISC) |
| `.card`, `.card-raised`, `.btn-primary`… | `Tarjeta`, `TarjetaElevada`, `BotonPrimario`… (`ui/componentes/`) |
| `Headline` con acento rojo | `Titular(texto, acento = "…")` |
| `MonthBars`, `CategoryBreakdown` | `BarrasMeses`, `DesgloseCategorias` |
| `formatMoney` con `Intl.NumberFormat("es-CL")` | `formatearDinero(valor, moneda, compacto)` |
| `parseAmount`, `formatTyping` | `parsearMonto`, `formatearMientrasEscribe` (`domain/Calculos.kt`, con los mismos casos de prueba) |
| Nav flotante en píldora | `BarraInferior` |

## Datos sin conexión

Los gastos y las categorías se guardan en Room como **caché**. Las pantallas leen siempre de la caché, así que funcionan sin red y muestran "Sin conexión. Mostrando datos guardados.". Las escrituras van primero al servidor, y solo si este acepta se actualiza la copia local. Si el servidor responde 401 (sesión vencida), la app vuelve sola al login. Al cerrar sesión se borra la caché de la cuenta, pero los ingresos quedan.

## Tests

| Tipo | Qué prueba |
|---|---|
| `CalculosTest` | Resumen del mes, proyección, comparación y lectura de montos (mismos casos que la web) |
| `GastosApiTest` | Cliente HTTP contra MockWebServer: token Bearer, 401, mensajes de error, JSON |
| `ViewModelsTest` | Inicio, Movimientos, formulario, Comparar, login y Ajustes con repositorios falsos |
| `NavegacionTest` (emulador) | App completa con un backend falso (`FakeGastosApi`): login, registrar, editar y eliminar, ingreso, sin conexión, cerrar sesión y bloqueo |
| `MigracionTest` | BD 1→2 y 2→3 (los ingresos se conservan) |

## Lecciones de esta etapa

- **Comentarios anidados en Kotlin.** Escribir una ruta como `/api/app/` seguida de un asterisco dentro de un `/** … */` abre un comentario nuevo que nunca se cierra. El error ("Unclosed comment") aparecía como un misterioso "could not be resolved" de KSP.
- **Lo que está debajo de la navegación flotante.** Un test que "toca" un botón al final de la pantalla puede tocar la píldora de navegación, que se dibuja encima. Los tests usan la **acción de clic semántica** (la misma que TalkBack), que no depende de qué hay dibujado arriba.
- **Probar con distintas pantallas.** Las 12 barras de Comparar partían "Nov" en dos líneas en pantallas chicas. Se vio con capturas y se corrigió con texto de una sola línea.
