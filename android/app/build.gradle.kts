plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Environments: docs/Architecture/12_dev-and-test-environment.md §1, §2.
private val region = "europe-west3"

android {
    namespace = "app.snackshot"
    compileSdk {
        version = release(37) { minorApiLevel = 2 } // Compose 1.13 needs 37.1+
    }

    defaultConfig {
        applicationId = "app.snackshot"
        minSdk = 28 // Health Connect minimum
        targetSdk = 37
        // CI passes -PversionCode=<run number> so every distributed build is newer than the last.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    flavorDimensions += "env"
    productFlavors {
        create("local") {
            dimension = "env"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            resValue("string", "app_name", "Snackshot Local")
            buildConfigField("String", "BROKER_BASE_URL", "\"http://localhost:5001/demo-snackshot/$region/api\"")
            buildConfigField("String", "LOGMEAL_BASE_URL", "\"http://localhost:8090\"")
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "true")
            buildConfigField("String", "APP_CHECK_PROVIDER", "\"none\"")
        }
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Snackshot Dev")
            buildConfigField("String", "BROKER_BASE_URL", "\"https://$region-snackshot-dev-app.cloudfunctions.net/api\"")
            buildConfigField("String", "LOGMEAL_BASE_URL", "\"https://api.logmeal.com\"")
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")
            buildConfigField("String", "APP_CHECK_PROVIDER", "\"debug\"")
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "Snackshot")
            buildConfigField("String", "BROKER_BASE_URL", "\"https://$region-snackshot-prod.cloudfunctions.net/api\"")
            buildConfigField("String", "LOGMEAL_BASE_URL", "\"https://api.logmeal.com\"")
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")
            buildConfigField("String", "APP_CHECK_PROVIDER", "\"playIntegrity\"")
        }
    }

    // Release signing comes only from CI (distribute-android.yml): dev builds use the dev key
    // from the GitHub `dev` environment, prod builds the Play upload key from `prod`.
    val keystorePath = providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("ci") {
                storeFile = file(keystorePath)
                storeType = "pkcs12"
                storePassword = providers.environmentVariable("SIGNING_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("ci")
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkDependencies = true // lint the library modules through the app
        abortOnError = true
    }
}

// The local flavor talks to emulators over cleartext HTTP; it never gets a release build.
androidComponents {
    beforeVariants { variant ->
        if (variant.buildType == "release" && variant.productFlavors.contains("env" to "local")) {
            variant.enable = false
        }
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":domain"))
    implementation(project(":data-local"))
    implementation(project(":logmeal"))
    implementation(project(":healthconnect"))
    implementation(project(":access"))
    implementation(project(":metrics"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
