# Control de Gastos — Roadmap Android con Kotlin

App Android nativa para registrar ingresos y gastos y consultar la cotización del dólar en Argentina. Es el **proyecto integrador** de un roadmap de formación en desarrollo Android con Kotlin: la misma app crece fase a fase y cada fase suma una capa (UI → datos → arquitectura → seguridad).

| Inicio | Movimientos | Dólar (sin conexión) |
|---|---|---|
| Saldo, gasto por categoría y equivalente en USD | Lista con filtros por tipo | Cotizaciones guardadas con aviso |

## Funcionalidades

- Registrar ingresos y gastos con categoría, con validación del formulario.
- Ver el saldo, el total de ingresos y gastos, y el gasto por categoría.
- Filtrar movimientos, ver el detalle y eliminarlos con confirmación.
- Cotizaciones del dólar (oficial, blue, MEP, CCL…) desde [DolarApi](https://dolarapi.com), con caché local: funciona sin conexión.
- Saldo convertido a dólares al tipo de cambio oficial.
- Modo claro y oscuro (Material 3).

## Tecnologías

| Área | Herramientas |
|---|---|
| Lenguaje | Kotlin 2.4, corrutinas, Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose (rutas type-safe) |
| Arquitectura | MVVM (ViewModel + StateFlow), capas domain / data / ui, Hilt |
| Datos | Room (SQLite), Retrofit, OkHttp, kotlinx.serialization |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, MockWebServer, Compose UI Test, Hilt Testing |
| Build | Gradle (Kotlin DSL) con catálogo de versiones, AGP 9, KSP |

## Estado del roadmap

| Fase | Tema | Estado | Guía |
|---|---|---|---|
| 0 | Kotlin desde cero: 15 ejercicios resueltos con tests | ✅ | [docs/FASE-0-kotlin.md](docs/FASE-0-kotlin.md) |
| 1 | Conceptos de Android: proyecto, Gradle, ciclo de vida | ✅ | [docs/FASE-1-android.md](docs/FASE-1-android.md) |
| 2 | UI con Jetpack Compose: multipantalla y navegación | ✅ | [docs/FASE-2-compose.md](docs/FASE-2-compose.md) |
| 3 | Datos, red y persistencia: Room, Retrofit, offline-first | ✅ | [docs/FASE-3-datos.md](docs/FASE-3-datos.md) |
| 4 | Arquitectura: MVVM, capas domain/data/ui, Hilt, tests de ViewModels | ✅ | [docs/FASE-4-arquitectura.md](docs/FASE-4-arquitectura.md) |
| 5 | Contexto bancario: seguridad, CI/CD, publicación | 🚧 Siguiente | |

Cada guía explica qué se hizo y por qué, e incluye ejercicios de práctica.

## Estructura

```
├── app/                         App Android
│   ├── src/main/java/.../
│   │   ├── domain/              Kotlin puro: modelos, interfaces de repositorio, casos de uso
│   │   ├── data/                Room (local/), Retrofit (remote/), repositorios, módulos Hilt (di/)
│   │   └── ui/                  Pantallas Compose + ViewModel por pantalla, navegación, tema
│   ├── src/test/                Tests unitarios (JVM)
│   ├── src/androidTest/         Tests instrumentados (emulador)
│   └── schemas/                 Esquemas exportados de Room
├── fase0-kotlin/                Ejercicios de Kotlin puro (Fase 0)
└── docs/                        Guía de estudio de cada fase
```

## Cómo ejecutarlo

Requisitos: Android Studio (versión reciente) con el SDK de Android 37, y un emulador o un dispositivo con Android 8.0 (API 26) o superior.

1. Abre la carpeta del proyecto en Android Studio y espera a que Gradle sincronice.
2. Elige el emulador y pulsa ▶ **Run 'app'**.

Desde la terminal:

```bash
./gradlew :app:installDebug              # compila e instala en el dispositivo conectado
./gradlew :app:testDebugUnitTest         # tests unitarios
./gradlew :app:connectedDebugAndroidTest # tests instrumentados (requiere emulador)
./gradlew :fase0-kotlin:run              # demo de los ejercicios de la Fase 0
./gradlew :fase0-kotlin:test             # tests de los ejercicios
```

## Licencia

Proyecto de formación personal.
