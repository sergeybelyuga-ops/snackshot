plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

// One entry point for CI: local-flavor app tests, library debug tests, pure-JVM tests.
tasks.register("unitTests") {
    group = "verification"
    description = "Runs all JVM unit tests for the local debug configuration."
    dependsOn(
        ":app:testLocalDebugUnitTest",
        ":ui:testDebugUnitTest",
        ":data-local:testDebugUnitTest",
        ":healthconnect:testDebugUnitTest",
        ":access:testDebugUnitTest",
        ":metrics:testDebugUnitTest",
        ":domain:test",
        ":logmeal:test",
    )
}
