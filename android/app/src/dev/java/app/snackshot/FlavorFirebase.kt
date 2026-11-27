package app.snackshot

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * dev: App Check debug tokens. Builds from App Distribution aren't installed from Play, so Play
 * Integrity can't vouch for them. On first launch the SDK logs a debug secret
 * ("Enter this debug secret into the allow list…"); register it once per install under App Check →
 * Apps → Manage debug tokens in snackshot-dev-app. Never in a prod build.
 */
internal object FlavorFirebase {
    fun appCheckFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
