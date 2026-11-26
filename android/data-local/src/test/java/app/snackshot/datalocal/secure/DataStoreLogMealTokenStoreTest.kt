package app.snackshot.datalocal.secure

import app.snackshot.datalocal.testDataStore
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The AES-GCM blob handling with a software key. The Android Keystore itself is not available
 * on the JVM; [KeystoreKeys] is exercised on the device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreLogMealTokenStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val token = "0123456789abcdef-test-token"

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private suspend fun TestScope.store(file: File, key: SecretKey, block: suspend (DataStoreLogMealTokenStore) -> Unit) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val dataStore = testDataStore(file, scope)
        try {
            block(DataStoreLogMealTokenStore(dataStore, AesGcmCipher { key }))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `token round-trips and only ciphertext reaches the disk`() = runTest {
        val file = tmp.root.resolve("secrets.preferences_pb")
        val key = newKey()
        store(file, key) { it.write(token) }
        assertFalse("Plaintext token found on disk", String(file.readBytes(), Charsets.ISO_8859_1).contains(token))
        store(file, key) { assertEquals(token, it.read()) }
    }

    @Test
    fun `no token on a fresh install`() = runTest {
        store(tmp.root.resolve("a.preferences_pb"), newKey()) { assertNull(it.read()) }
    }

    @Test
    fun `a token made with another key is dropped so the app provisions again`() = runTest {
        val file = tmp.root.resolve("b.preferences_pb")
        store(file, newKey()) { it.write(token) }
        val other = newKey()
        store(file, other) { assertNull(it.read()) }
        store(file, other) { assertNull(it.read()) } // cleared, not retried every time
    }

    @Test
    fun `clear removes the token`() = runTest {
        val file = tmp.root.resolve("c.preferences_pb")
        val key = newKey()
        store(file, key) {
            it.write(token)
            it.clear()
            assertNull(it.read())
        }
    }

    @Test
    fun `each write uses a fresh IV`() {
        val cipher = AesGcmCipher(newKey().let { k -> { k } })
        val aad = "aad".toByteArray()
        val a = cipher.encrypt(token.toByteArray(), aad)
        val b = cipher.encrypt(token.toByteArray(), aad)
        assertFalse(a.contentEquals(b))
        assertEquals(token, String(cipher.decrypt(a, aad)))
    }

    @Test(expected = java.security.GeneralSecurityException::class)
    fun `a blob cannot be read with other associated data`() {
        val cipher = AesGcmCipher(newKey().let { k -> { k } })
        cipher.decrypt(cipher.encrypt(token.toByteArray(), "a".toByteArray()), "b".toByteArray())
    }
}
