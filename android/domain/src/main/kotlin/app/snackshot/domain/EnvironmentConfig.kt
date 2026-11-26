package app.snackshot.domain

/** App Check attestation used by the broker client (doc 12 §1). */
enum class AppCheckProvider {
    NONE,
    DEBUG,
    PLAY_INTEGRITY,
    ;

    companion object {
        fun fromBuildConfig(value: String): AppCheckProvider = when (value) {
            "none" -> NONE
            "debug" -> DEBUG
            "playIntegrity" -> PLAY_INTEGRITY
            else -> throw IllegalArgumentException("Unknown App Check provider: $value")
        }
    }
}

/**
 * Environment settings for one flavor (`local`, `dev`, `prod`).
 * Built once from BuildConfig in the app module; everything else injects this.
 */
data class EnvironmentConfig(
    val name: String,
    val brokerBaseUrl: String,
    val logmealBaseUrl: String,
    val useFirebaseEmulators: Boolean,
    val appCheckProvider: AppCheckProvider,
    /** Test builds (local, dev) show the Atwater comparison next to LogMeal's energy (doc 03 §5). */
    val showAtwaterComparison: Boolean,
) {
    init {
        if (!useFirebaseEmulators) {
            require(brokerBaseUrl.startsWith("https://")) { "Broker URL must use HTTPS outside the emulator: $brokerBaseUrl" }
            require(logmealBaseUrl.startsWith("https://")) { "LogMeal URL must use HTTPS outside the emulator: $logmealBaseUrl" }
            require(appCheckProvider != AppCheckProvider.NONE) { "App Check is required outside the emulator" }
        }
        require(!(showAtwaterComparison && appCheckProvider == AppCheckProvider.PLAY_INTEGRITY)) {
            "The Atwater comparison is for test builds only"
        }
    }
}
