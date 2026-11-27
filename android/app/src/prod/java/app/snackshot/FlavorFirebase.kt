package app.snackshot

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** prod: Play Integrity (doc 11 §8). */
internal object FlavorFirebase {
    fun appCheckFactory(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
