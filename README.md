# Control de Gastos — Roadmap Android con Kotlin

[![CI](https://github.com/1loro/kotlinroadmap/actions/workflows/ci.yml/badge.svg)](https://github.com/1loro/kotlinroadmap/actions/workflows/ci.yml)

App Android nativa para registrar ingresos y gastos y consultar la cotización del dólar en Argentina, con prácticas de seguridad de una app bancaria. Es el **proyecto integrador** de un roadmap de formación en desarrollo Android con Kotlin: la misma app crece fase a fase y cada fase suma una capa (UI → datos → arquitectura → seguridad).

| Inicio | Movimientos | Dólar (sin conexión) |
|---|---|---|
| Saldo, gasto por categoría y equivalente en USD | Lista con filtros por tipo | Cotizaciones guardadas con aviso |

## Funcionalidades

- Registrar ingresos y gastos con categoría, con validación del formulario.
- Ver el saldo, el total de ingresos y gastos, y el gasto por categoría.
- Filtrar movimientos, ver el detalle y eliminarlos con confirmación.
- Cotizaciones del dólar (oficial, blue, MEP, CCL…) desde [DolarApi](https://dolarapi.com), con caché local: funciona sin conexión.
- Saldo convertido a dólares al tipo de cambio oficial.
- Bloqueo con huella o PIN del dispositivo.
- Modo claro y oscuro (Material 3).

## Seguridad

| Medida | Detalle |
|---|---|
| Datos cifrados | Base de datos con SQLCipher (AES-256); la clave es aleatoria y la protege el **Android Keystore** |
| Bloqueo de la app | Biometría o credencial del dispositivo al abrir y tras 30 s en segundo plano |
| Red | Solo HTTPS y solo certificados del sistema (`network_security_config`) |
| Privacidad | Sin capturas ni miniatura en Recientes (`FLAG_SECURE`, en release) y sin backups |
| Código | R8 ofusca toda la app; los logs se eliminan de la release |
| Secretos | Claves de firma fuera del repo (`keystore.properties` o secrets de CI) |

Detalle y relación con el OWASP Mobile Top 10: [docs/FASE-5-profesional.md](docs/FASE-5-profesional.md).

## Tecnologías

| Área | Herramientas |
|---|---|
| Lenguaje | Kotlin 2.4, corrutinas, Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose (rutas type-safe) |
| Arquitectura | MVVM (ViewModel + StateFlow), capas domain / data / ui, Hilt |
| Datos | Room + SQLCipher, Retrofit, OkHttp, kotlinx.serialization |
| Seguridad | Android Keystore, BiometricPrompt, Network Security Config, R8 |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, MockWebServer, Compose UI Test, Hilt Testing |
| Build y CI | Gradle (Kotlin DSL) con catálogo de versiones, AGP 9, KSP, GitHub Actions |

## Estado del roadmap

| Fase | Tema | Estado | Guía |
|---|---|---|---|
| 0 | Kotlin desde cero: 15 ejercicios resueltos con tests | ✅ | [docs/FASE-0-kotlin.md](docs/FASE-0-kotlin.md) |
| 1 | Conceptos de Android: proyecto, Gradle, ciclo de vida | ✅ | [docs/FASE-1-android.md](docs/FASE-1-android.md) |
| 2 | UI con Jetpack Compose: multipantalla y navegación | ✅ | [docs/FASE-2-compose.md](docs/FASE-2-compose.md) |
| 3 | Datos, red y persistencia: Room, Retrofit, offline-first | ✅ | [docs/FASE-3-datos.md](docs/FASE-3-datos.md) |
| 4 | Arquitectura: MVVM, capas domain/data/ui, Hilt, tests de ViewModels | ✅ | [docs/FASE-4-arquitectura.md](docs/FASE-4-arquitectura.md) |
| 5 | Contexto bancario: Git en equipo, seguridad, CI/CD, publicación | ✅ | [docs/FASE-5-profesional.md](docs/FASE-5-profesional.md) |

Cada guía explica qué se hizo y por qué, e incluye ejercicios de práctica.

## Estructura

```
├── app/                         App Android
│   ├── src/main/java/.../
│   │   ├── domain/              Kotlin puro: modelos, interfaces de repositorio, casos de uso
│   │   ├── data/                Room (local/), Retrofit (remote/), repositorios, Keystore (seguridad/), Hilt (di/)
│   │   └── ui/                  Pantallas Compose + ViewModel por pantalla, navegación, bloqueo (seguridad/), tema
│   ├── src/debug/               Recursos solo de la versión debug
│   ├── src/test/                Tests unitarios (JVM)
│   ├── src/androidTest/         Tests instrumentados (emulador)
│   └── schemas/                 Esquemas exportados de Room
├── fase0-kotlin/                Ejercicios de Kotlin puro (Fase 0)
├── docs/                        Guía de estudio de cada fase
└── .github/                     CI (workflows/ci.yml) y plantilla de Pull Request
```

## Cómo ejecutarlo

Requisitos: Android Studio (versión reciente) con el SDK de Android 37, y un emulador o un dispositivo con Android 8.0 (API 26) o superior. Para el bloqueo, el dispositivo debe tener configurado un PIN, patrón o huella (si no tiene ninguno, la app no se bloquea).

1. Abre la carpeta del proyecto en Android Studio y espera a que Gradle sincronice.
2. Elige el emulador y pulsa ▶ **Run 'app'**. Se instala "Gastos (debug)".

Desde la terminal:

```bash
./gradlew :app:installDebug              # instala la versión debug
./gradlew :app:testDebugUnitTest         # tests unitarios
./gradlew :app:connectedDebugAndroidTest # tests instrumentados (requiere emulador)
./gradlew :app:lintDebug                 # análisis estático
./gradlew :app:bundleRelease             # App Bundle de release (ver firma en docs/FASE-5)
./gradlew :fase0-kotlin:run              # demo de los ejercicios de la Fase 0
```

Configuración opcional: `-PdolarApiUrl=https://...` (o la variable de entorno `DOLAR_API_URL`) cambia el servidor de cotizaciones.

## Tests

| Tipo | Qué cubre |
|---|---|
| Unitarios (JVM) | Casos de uso, los 6 ViewModels, repositorio contra un servidor HTTP falso, lógica de resumen, ejercicios de la Fase 0 |
| Instrumentados | Room con SQLite real, Keystore y BD cifrada, y flujos completos de UI con Hilt (agregar, filtrar, cotizaciones, sin conexión, bloqueo) |

## Licencia

Proyecto de formación personal.
