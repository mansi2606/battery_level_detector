plugins {
    id("com.android.application")
}

android {
    namespace = "com.first.bluetoothconnectivity"  // ← your real package name
    compileSdk = 34

    defaultConfig {
        applicationId = "com.first.bluetoothconnectivity"  // ← same here
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // removed kotlinOptions block — not needed without explicit kotlin plugin
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}