import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    
    android {
       namespace = "com.toneatti.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)

            // Agregada para autenticación: motor HTTP de Android para Ktor.
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // ==================== AGREGADAS EN ESTA REFACTORIZACIÓN ===================
            // Dependencias nuevas para la BottomBar de navegación entre 3 pantallas:
            //   - Navegación entre pestañas (NavHost + NavController de JetBrains).
            implementation(libs.androidx.navigation.compose)
            //   - Iconos Material de las pestañas (Icons.Filled.Search/DateRange/Settings).
            implementation(libs.compose.material.iconsCore)
            // ==========================================================================

            // ===================== AGREGADAS PARA AUTENTICACIÓN =======================
            // Cliente HTTP contra Servicio-turnos con cuerpos JSON (kotlinx-serialization).
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            // ==========================================================================
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Agregadas para autenticación: servidor HTTP simulado y Dispatchers.setMain.
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}