// File: build.gradle.kts (Project-level)
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()

    }
}