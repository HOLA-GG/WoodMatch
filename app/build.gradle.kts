import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// El prototipo funciona con datos locales. Firebase se activa automáticamente
// cuando cada entorno incorpora su propio app/google-services.json (ignorado por Git).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.kotlinapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kotlinapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "2.0.1-pilot"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Only public endpoint configuration belongs in the APK.
        val endpoint = providers.gradleProperty("WOODMATCH_API_URL").orElse(providers.environmentVariable("WOODMATCH_API_URL")).getOrElse("")
        require(endpoint.isEmpty() || endpoint.matches(Regex("https://[a-z0-9-]+\\.supabase\\.co/functions/v1/woodmatch")))
        buildConfigField("String", "WOODMATCH_API_URL", "\"$endpoint\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        buildConfigField("String", "PLANTNET_API_KEY", "\"\"")

    }

    defaultConfig { applicationId = "com.example.kotlinapp.pilot" }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    // FragmentActivity must support ActivityResultRegistry request codes (Fragment >= 1.3).
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Location & Google Maps SDK
    implementation(libs.play.services.location)

    // Firebase Auth & Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation(libs.coil.compose)

    // Generative AI (Gemini) SDK
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // OpenStreetMap Android SDK
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
