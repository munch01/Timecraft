import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.auth)
                implementation(libs.supabase.realtime)
                
                implementation(libs.navigation.compose)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.biometric)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.maplibre.android)
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

android {
    namespace = "com.emeric.timecraft"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emeric.timecraft"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.3"
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
                ?: localProperties.getProperty("KEYSTORE_PATH")
                ?: "release.keystore"
            val kFile = file(keystorePath)
            val kPass = System.getenv("KEYSTORE_PASSWORD") ?: localProperties.getProperty("KEYSTORE_PASSWORD")
            val kAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("KEY_ALIAS")
            val kKeyPass = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD")

            if (kFile.exists() && !kPass.isNullOrEmpty() && !kAlias.isNullOrEmpty() && !kKeyPass.isNullOrEmpty()) {
                storeFile = kFile
                storePassword = kPass
                keyAlias = kAlias
                keyPassword = kKeyPass
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile?.exists() == true && !releaseConfig.storePassword.isNullOrEmpty()) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TimeCraft"
            packageVersion = "1.0.0"
        }
    }
}
