plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" // Add KSP version here
}

android {
    namespace = "com.essential.essspace"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.essential.essspace"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Updated to match kotlin version
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Use the same Compose BOM as your old working setup
    implementation(platform("androidx.compose:compose-bom:2024.05.00")) // Or "2024.04.00", "2024.02.02"
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.animation:animation")     // And this

    // Keep other dependencies as they were in your "old working" app-level build.gradle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.activity:activity-compose:1.8.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    val roomVersion = "2.6.1" // Or 2.6.0 if that was in your old working one
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    val cameraXVersion = "1.3.0" // From your old working one
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("io.coil-kt:coil-compose:2.4.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00")) // Match the main BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}