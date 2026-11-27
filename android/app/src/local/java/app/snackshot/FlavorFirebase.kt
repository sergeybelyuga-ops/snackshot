package app.snackshot

import com.google.firebase.appcheck.AppCheckProviderFactory

/** local: the emulator broker doesn't check App Check (functions/.env.demo-snackshot). */
internal object FlavorFirebase {
    fun appCheckFactory(): AppCheckProviderFactory? = null
}
