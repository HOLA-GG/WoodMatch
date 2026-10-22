import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// El prototipo funciona con datos locales. Firebase se activa automáticamente
// cuando cada entorno incorpora su propio app/google-services.json (ignorado por Git).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.isFile) {
    envFile.inputStream().use { stream ->
        envProperties.load(stream)
    }
}

// AI Studio puede exportar secretos como archivos individuales dentro de .env/.
// Se leen solo durante la compilación y toda la carpeta está excluida de Git.
fun secretFileValue(name: String): String? {
    val secretFile = rootProject.file(".env/$name")
    return secretFile.takeIf { it.isFile }
        ?.readText()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
val plantNetApiKey = envProperties.getProperty("PLANTNET_API_KEY")
    ?: System.getenv("PLANTNET_API_KEY")
    ?: secretFileValue("PLANTNET_API_KEY")
    ?: ""

val geminiApiKey = envProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: secretFileValue("GEMINI_API_KEY")
    ?: ""

val mapsApiKey = envProperties.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""

android {
    namespace = "com.example.kotlinapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kotlinapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "PLANTNET_API_KEY", "\"$plantNetApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Location & Google Maps SDK
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Firebase Auth & Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
