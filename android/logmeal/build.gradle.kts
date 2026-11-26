import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// LogMeal REST adapter. Pure Kotlin/JVM (OkHttp is added with the client in WBS 4.x).
plugins {
    alias(libs.plugins.kotlin.jvm)
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
    implementation(libs.hilt.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
