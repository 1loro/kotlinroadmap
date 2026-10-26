# Fase 1 — Conceptos de desarrollo Android

Entregable cumplido: proyecto "Hola Mundo" ejecutado en el emulador `Medium_Phone_API_37.0`.

## Estructura del proyecto

```
proyectos/
├── settings.gradle.kts      ← qué módulos forman el proyecto (:app, :fase0-kotlin)
├── build.gradle.kts         ← plugins comunes (apply false = disponibles, no aplicados)
├── gradle/libs.versions.toml← catálogo de versiones: TODAS las dependencias en un lugar
├── gradle.properties        ← opciones de Gradle (memoria, caché)
└── app/
    ├── build.gradle.kts     ← config del módulo Android: SDKs, applicationId, dependencias
    └── src/main/
        ├── AndroidManifest.xml  ← "DNI" de la app: Activities, permisos, tema, ícono
        ├── java/com/example/controlgastos/
        │   ├── MainActivity.kt  ← punto de entrada (LAUNCHER en el manifest)
        │   └── ui/theme/Theme.kt← colores Material 3
        └── res/                 ← recursos: strings, íconos (mipmap-*dpi), XML
```

**Gradle** es el sistema de build: descarga dependencias, compila Kotlin, empaqueta recursos y firma el APK. **AGP** (Android Gradle Plugin) le enseña a Gradle a construir apps Android.

- `minSdk = 24`: la versión de Android más antigua soportada (Android 7.0).
- `targetSdk = 37`: la versión para la que se probó la app (activa los comportamientos nuevos).
- `compileSdk = 37`: las APIs disponibles al compilar.

## Ciclo de vida de una Activity

`MainActivity` registra cada transición en Logcat. Filtra por `CicloDeVida` en la pestaña Logcat de Android Studio.

```
onCreate → onStart → onResume   (app en pantalla, interactiva)
          ↑            ↓
       onRestart    onPause      (algo la tapa parcialmente)
          ↑            ↓
          └──────── onStop       (ya no es visible)
                       ↓
                   onDestroy     (se cierra, o se ROTA la pantalla)
```

Ya verificado al abrir la app: `onCreate → onStart → onResume`.

## Diseño móvil (Material Design)

- **dp** (density-independent pixels) para tamaños y **sp** para texto (respeta el tamaño de fuente del usuario). Nunca uses px.
- Los objetivos táctiles deben medir **48 dp como mínimo** (ergonomía).
- Las carpetas `mipmap-mdpi … xxxhdpi` contienen el mismo ícono para cada densidad de pantalla.
- **Material 3** define roles de color (`primary`, `surface`, `error`…) en lugar de colores sueltos, lo que da modo oscuro "gratis".

## Permisos, almacenamiento y conectividad

- Los permisos se declaran en el manifest (`<uses-permission>`). Los "peligrosos" (cámara, ubicación) además se piden en tiempo de ejecución. `INTERNET` es normal: no requiere pedirlo al usuario (lo usaremos en la Fase 3).
- Almacenamiento: **interno privado** (archivos y BD de la app, que nadie más ve; aquí irá Room), **DataStore** para preferencias, y **almacenamiento compartido** solo para archivos del usuario.
- Conectividad: las peticiones de red nunca van en el hilo principal (corrutinas) y Android bloquea HTTP sin cifrar por defecto.

## Práctica (hazlo tú)

1. Ejecuta la app desde Android Studio (▶) y abre Logcat filtrando por `CicloDeVida`.
2. Pulsa *Home* y vuelve a la app: ¿qué métodos se llaman?
3. **Rota el emulador.** Verás `onDestroy` y luego `onCreate` otra vez. La Activity se destruye y se recrea en cada rotación, y por eso en la Fase 4 usaremos `ViewModel`.
4. Cambia `Saludo("Mundo")` por tu nombre y mira el `@Preview` en la vista *Split* sin ejecutar la app.
5. Cambia el color `primary` de `LightColors` en `Theme.kt` y activa el modo oscuro del emulador.
