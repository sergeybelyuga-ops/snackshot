// Anonymous daily product metrics sent through the broker. WBS I-8.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.snackshot.metrics"
    compileSdk {
        version = release(37) { minorApiLevel = 2 } // Compose 1.13 needs 37.1+
    }
    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
}
