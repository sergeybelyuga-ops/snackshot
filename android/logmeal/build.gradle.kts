import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// LogMeal REST adapter (WBS 6.3). Pure Kotlin/JVM: OkHttp, kotlinx.serialization, no Android.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// Shared recorded LogMeal responses (docs/Architecture/12_dev-and-test-environment.md §3.1).
sourceSets {
    test {
        resources.srcDir(rootDir.resolve("../test-fixtures"))
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.test {
    useJUnitPlatform()
}
