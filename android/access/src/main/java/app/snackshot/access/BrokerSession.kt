package app.snackshot.access

import app.snackshot.access.broker.BrokerAuth

/**
 * The caller identity for broker calls: a silent anonymous Firebase user plus App Check
 * (doc 11 §5.3, §8). There is no sign-in or sign-out UI.
 */
interface BrokerSession {
    /**
     * Signs in anonymously on first use and returns fresh tokens.
     *
     * @param forceRefresh after the broker rejected the ID token.
     * @throws java.io.IOException without a connection.
     */
    suspend fun auth(forceRefresh: Boolean): BrokerAuth

    /** This install has an anonymous user already. Never calls the network. */
    val isSignedIn: Boolean
}
