# Fase 5 — Contexto profesional / bancario

Entregable: app final pulida con README, pruebas y buenas prácticas de seguridad.

## 1. Git en equipo

Esta fase se desarrolló como en un equipo real: en la rama `fase-5-seguridad`, integrada a `main` con un **Pull Request**.

```
main ──●──────────────────────────●──  (siempre estable, protegida)
        \                        /
         ●──●──●──●  fase-5-seguridad   ← commits pequeños, PR, revisión, CI verde
```

| Práctica | Cómo se aplica aquí |
|---|---|
| Una rama por tarea | `fase-5-seguridad`. En un banco: `feature/JIRA-123-bloqueo-biometrico` |
| Commits pequeños con mensaje claro | Qué cambió y **por qué** (ver `git log`) |
| Pull Request con plantilla | `.github/pull_request_template.md`: qué cambia, cómo probarlo, checklist de seguridad |
| CI obligatorio | `.github/workflows/ci.yml` corre tests y Lint en cada PR |
| Code review | Otra persona aprueba antes de integrar. En GitHub: *Settings → Branches → Branch protection* para exigir PR + CI verde + 1 aprobación en `main` |

**Al revisar un PR, mira:** ¿hace lo que dice? ¿tiene tests? ¿hay secretos o logs con datos? ¿maneja errores (sin red, datos vacíos)? ¿se entiende sin explicación oral?

## 2. Seguridad móvil

| Medida | Dónde | Riesgo OWASP Mobile Top 10 (2024) |
|---|---|---|
| Base de datos cifrada (SQLCipher, AES-256) | `GastosDatabase.construirCifrada` | M9 Almacenamiento inseguro de datos |
| Clave de la BD protegida por el **Android Keystore** | `data/seguridad/GestorClaves.kt` | M9, M10 Criptografía insuficiente |
| Bloqueo con biometría o PIN del dispositivo | `ui/seguridad/` + `MainActivity` | M3 Autenticación/autorización insegura |
| Solo HTTPS; sin certificados instalados por el usuario | `res/xml/network_security_config.xml` | M5 Comunicación insegura |
| Sin backups ni transferencia de datos | `allowBackup="false"`, `data_extraction_rules.xml` | M9 |
| Sin capturas ni miniatura en Recientes (release) | `FLAG_SECURE` en `MainActivity` | M6 Controles de privacidad inadecuados |
| Logs solo en debug y eliminados por R8 | `MainActivity.log()`, `keepRules/rules.keep` | M6 |
| Ofuscación de todo el código con R8 | `optimization { enable = true }` | M7 Protección binaria insuficiente |
| Validación de entradas | `AgregarMovimientoUseCase` | M4 Validación insuficiente de entradas |
| Secretos fuera del repo | `.gitignore`, secrets de CI | M1 Uso indebido de credenciales |
| Dependencias actualizadas | `libs.versions.toml` | M2 Seguridad insuficiente de la cadena de suministro |

### Cómo funciona el cifrado

```
Android Keystore (hardware seguro)          archivos de la app
┌──────────────────────────┐   cifra   ┌─────────────────────────────┐
│ clave maestra AES-256    │ ────────► │ claves_cifradas.xml          │
│ (no se puede extraer)    │ ◄──────── │   clave_bd = IV + cifrado    │
└──────────────────────────┘  descifra └─────────────────────────────┘
                                               │ 32 bytes aleatorios
                                               ▼
                                       gastos_cifrada.db (SQLCipher)
```

- La clave de la BD es **aleatoria** (no una contraseña fija en el código, que cualquiera vería al decompilar).
- Si roban el archivo de la BD, sin el Keystore de *ese* teléfono es ilegible. `SeguridadTest` lo comprueba: el archivo no empieza con la cabecera `SQLite format 3`.
- `EncryptedSharedPreferences` (que el roadmap menciona) está **deprecada desde 2025**. La alternativa recomendada es usar el Keystore directamente, como hace `GestorClaves`.
- **Migración:** al pasar a la BD cifrada, la BD sin cifrar de las fases 3 y 4 se descarta. En una app publicada se migraría con `ATTACH ... KEY` + `SELECT sqlcipher_export(...)` para no perder datos.

### Bloqueo biométrico

- Se pide al abrir la app y al volver después de **30 s** en segundo plano (`BloqueoViewModel.TIEMPO_GRACIA_MS`). Rotar la pantalla no vuelve a pedirlo.
- Acepta huella o rostro, o en su defecto el PIN, patrón o contraseña del dispositivo.
- Mientras está bloqueada, la app sigue debajo (no pierde la pantalla en la que estabas), pero sin semántica: TalkBack no puede leer los saldos.
- `Autenticador` es una interfaz: en los tests, Hilt la reemplaza por `FakeAutenticador`.

### Certificate pinning (conceptual)

Fijar el certificado del servidor impide un MITM aunque la autoridad certificante esté comprometida. Las apps bancarias lo usan **contra sus propios servidores**, cuya rotación de certificados controla el banco:

```xml
<domain-config>
    <domain includeSubdomains="true">api.mibanco.com</domain>
    <pin-set expiration="2027-06-01">
        <pin digest="SHA-256">base64-de-la-clave-publica=</pin>
        <pin digest="SHA-256">base64-de-la-clave-de-respaldo=</pin>
    </pin-set>
</domain-config>
```

No se aplicó a `mindicador.cl` porque es un servidor de terceros: si cambia su certificado sin aviso, la app dejaría de funcionar.

## 3. Configuración por entorno y BuildConfig

| Qué | Cómo |
|---|---|
| URL de la API | `BuildConfig.INDICADORES_API_URL`. Se cambia sin tocar código: `./gradlew assembleDebug -PindicadoresApiUrl=https://staging...` o la variable de entorno `INDICADORES_API_URL` |
| Comportamiento por tipo de build | `BuildConfig.BLOQUEAR_CAPTURAS`: `false` en debug, `true` en release |
| Debug y release en el mismo teléfono | `applicationIdSuffix = ".debug"`; la debug se llama "Gastos (debug)" (`src/debug/res`) |
| Red en debug | `debug-overrides` acepta certificados de usuario para usar un proxy de inspección |

> **Ojo:** `BuildConfig` **no** es un lugar seguro para secretos. Todo lo que va en el APK se puede extraer. Las claves de APIs privadas viven en el servidor, nunca en la app.

## 4. CI/CD

`.github/workflows/ci.yml` tiene tres jobs:

1. **build** (en cada PR y push): tests unitarios + Lint + APK debug. Sube los reportes como artefacto.
2. **tests-instrumentados**: levanta un emulador en la máquina de GitHub y corre los tests de UI.
3. **release** (solo en `main`, si los anteriores pasan): genera el `.aab` firmado con la clave guardada en los *secrets* del repositorio.

**Lección real de este proyecto.** La primera vez que el CI corrió los tests instrumentados, fallaron 2 que en local pasaban siempre. Pasos para encontrar la causa:
1. Los logs de Actions requieren iniciar sesión, así que se agregó un paso que publica los tests fallidos como **anotaciones** (`::error`), visibles en el PR.
2. Las anotaciones mostraron que no aparecían "Feria" ni "Arriendo", dos elementos al final de una lista.
3. Causa: un `LazyColumn` solo crea los elementos **visibles**. El emulador del CI tiene una pantalla más chica que la del emulador local, así que esos elementos quedaban fuera.
4. Se reprodujo en local achicando la pantalla (`adb shell wm size 720x1280`) y se corrigió el test para que **desplace la lista** hasta el elemento (`performScrollToNode`), como haría un usuario.

Moraleja: un test de UI no debe depender del tamaño de la pantalla, y el CI sirve justamente para descubrir lo que "en mi máquina funciona".

Esto es **CI** (integración continua). **CD** (entrega continua) sería agregar un paso que suba el `.aab` a Play Console, por ejemplo con la API de Google Play Developer, en el canal de pruebas internas.

## 5. Publicación

### Firmar la app

Toda app de Play Store se firma. La clave identifica al autor: **si se pierde, no se puede actualizar la app**.

```bash
# 1. Crear la clave de subida (una vez; guárdala con la contraseña en un gestor seguro)
keytool -genkeypair -v -keystore ~/claves/controlgastos-upload.jks \
  -alias upload -keyalg RSA -keysize 4096 -validity 10000

# 2. Crear keystore.properties en la raíz del proyecto (está en .gitignore)
storeFile=C:/Users/<tu-usuario>/claves/controlgastos-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...

# 3. Generar el App Bundle firmado
./gradlew :app:bundleRelease        # → app/build/outputs/bundle/release/app-release.aab
```

Sin `keystore.properties`, la release se firma con la clave de debug. Sirve para **probarla** localmente, pero Play Store la rechaza.

### App Bundle (.aab) vs APK

Play Store exige **App Bundle**. Google genera desde él APKs optimizados para cada dispositivo (solo su densidad de pantalla, su arquitectura y su idioma), así que la descarga es más chica. Con **Play App Signing**, Google guarda la clave final de firma y tú subes con una "clave de subida", que se puede reemplazar si se pierde.

### Proceso en Play Console (conceptual)

1. Cuenta de desarrollador (pago único).
2. Crear la app: nombre, categoría, política de privacidad (obligatoria si maneja datos personales o financieros).
3. Formulario de **Seguridad de los datos**: qué datos se recopilan y si se cifran.
4. Subir el `.aab` al canal de **pruebas internas**, luego cerradas, luego producción.
5. Revisión de Google y publicación (lanzamiento gradual: 5% → 20% → 100%).

**Antes de publicar falta:** cambiar `applicationId` (Play Store no acepta `com.example.*`; por ejemplo `io.github.<usuario>.controlgastos`) y subir `versionCode` en cada versión.

## Práctica (hazlo tú)

1. **Revisa el PR de esta fase** en GitHub como si fueras el revisor: deja al menos un comentario.
2. **Protege `main`:** *Settings → Branches → Add rule*: exige PR, CI verde y 1 aprobación.
3. **Firma la release:** crea tu keystore con `keytool`, configura `keystore.properties` y genera el `.aab`.
4. **Secrets de CI:** sube el keystore en base64 (`base64 -w0 controlgastos-upload.jks`) y las contraseñas como secrets del repo, y mira cómo el job `release` genera el `.aab` firmado.
5. **Comprueba el cifrado:** con el *Device File Explorer* de Android Studio, descarga `databases/gastos_cifrada.db` e intenta abrirla con *DB Browser for SQLite*. No vas a poder.
6. **Prueba `FLAG_SECURE`:** instala la release (`./gradlew :app:installRelease`) e intenta una captura de pantalla.
7. **Desafío:** migra la BD sin cifrar a cifrada sin perder datos con `sqlcipher_export`, y escribe el test.
