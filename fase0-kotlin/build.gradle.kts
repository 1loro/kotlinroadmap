import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de Kotlin puro (sin Android) para los ejercicios de la Fase 0.
// Ejecutar:  ./gradlew :fase0-kotlin:run
// Tests:     ./gradlew :fase0-kotlin:test
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

application {
    mainClass = "fase0.MainKt"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
