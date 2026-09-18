plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

android {
    namespace = "com.mmushtaq04.coupoop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mmushtaq04.coupoop"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation(libs.androidx_appcompat)
    implementation(libs.androidx_core_ktx)

    // Compose
    implementation(libs.activity_compose)
    implementation(libs.compose_ui)
    implementation(libs.material3_lib)

    // Compose BOM
    implementation(platform(libs.compose_bom_lib))

    // Firebase BOM and KTX libraries
    implementation(platform(libs.firebase_bom_lib))
    implementation(libs.firebase_auth_ktx)
    implementation(libs.firebase_firestore_ktx)
    implementation(libs.firebase_messaging_lib)

    // Optional Google Sign-In for account recovery
    implementation(libs.play_services_auth_lib)

    // Jetpack Glance for App Widget
    implementation(libs.glance_appwidget_lib)

    testImplementation(libs.junit_lib)
    androidTestImplementation(libs.androidx_junit_lib)
    androidTestImplementation(libs.espresso_core_lib)
}