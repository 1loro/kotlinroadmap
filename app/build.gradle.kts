import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
}

// --- Configuración externa (no se escribe en el código) ---
// URL de la API: se puede cambiar sin tocar código, por ejemplo para apuntar a
// un servidor de pruebas:  ./gradlew assembleDebug -PindicadoresApiUrl=https://...
// o con la variable de entorno INDICADORES_API_URL (así lo haría un pipeline de CI).
val indicadoresApiUrl: String = providers.gradleProperty("indicadoresApiUrl")
    .orElse(providers.environmentVariable("INDICADORES_API_URL"))
    .getOrElse("https://mindicador.cl/")

// Firma de release: los datos salen de keystore.properties (ignorado por Git)
// o de variables de entorno (secretos de CI). Ver docs/FASE-5-profesional.md.
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun datoFirma(clave: String, variable: String): String? =
    firma.getProperty(clave) ?: providers.environmentVariable(variable).orNull
val hayFirmaRelease = datoFirma("storeFile", "KEYSTORE_PATH") != null

android {
    namespace = "com.example.controlgastos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.controlgastos"
        // 26 (Android 8.0) habilita java.time (LocalDate) sin configuración extra.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Runner propio: arranca los tests con la Application de prueba de Hilt.
        testInstrumentationRunner = "com.example.controlgastos.HiltTestRunner"

        // Constantes que el código lee como BuildConfig.INDICADORES_API_URL.
        buildConfigField("String", "INDICADORES_API_URL", "\"$indicadoresApiUrl\"")
    }

    signingConfigs {
        if (hayFirmaRelease) {
            create("release") {
                storeFile = file(datoFirma("storeFile", "KEYSTORE_PATH")!!)
                storePassword = datoFirma("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = datoFirma("keyAlias", "KEY_ALIAS")
                keyPassword = datoFirma("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // La versión debug se instala JUNTO a la release (paquete distinto).
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // En debug se permiten capturas de pantalla (para desarrollar y testear).
            buildConfigField("boolean", "BLOQUEAR_CAPTURAS", "false")
        }
        release {
            buildConfigField("boolean", "BLOQUEAR_CAPTURAS", "true")
            // Sin keystore propio se firma con la clave de debug: sirve para
            // PROBAR la release localmente, nunca para publicarla.
            signingConfig = if (hayFirmaRelease) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            // R8: elimina código sin usar, optimiza y OFUSCA (renombra clases y
            // métodos) toda la app. La plantilla original limitaba R8 a las
            // librerías (packageScope) y dejaba nuestro código legible al
            // decompilar el APK.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room exporta el esquema de la BD a JSON en cada versión. Se versiona en Git
// y sirve para escribir y probar migraciones cuando cambie el esquema.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.sqlcipher.android)
    // Explícita para fijar UNA versión de corrutinas en la app y en los tests.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
