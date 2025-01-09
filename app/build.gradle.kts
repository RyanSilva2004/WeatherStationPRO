// File: app/build.gradle.kts (App-level)
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "lk.ryan.weatherstationpro"
    compileSdk = 34

    defaultConfig {
        applicationId = "lk.ryan.weatherstationpro"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.core:core:1.13.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.jjoe64:graphview:4.2.2")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.material)
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}