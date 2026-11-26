package app.snackshot.datalocal.images

import app.snackshot.datalocal.TEST_JPEG
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileMealImageStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dirs by lazy {
        ImageDirectories(processed = File(tmp.root, "files/meal-images"), captures = File(tmp.root, "cache/captures"))
    }
    private val store by lazy { FileMealImageStore(dirs) }

    @Test
    fun `processed image is stored under a reference and can be deleted`() = runTest {
        val ref = store.saveProcessed(TEST_JPEG)
        assertTrue(ref, ref.matches(Regex("meal-images/[0-9a-f-]{36}\\.jpg")))
        assertArrayEquals(TEST_JPEG, store.fileFor(ref)!!.readBytes())
        assertEquals(listOf(ref.substringAfter('/')), dirs.processed.list()!!.toList()) // no temp file left

        store.delete(ref)
        assertNull(store.fileFor(ref))
    }

    @Test
    fun `only JPEG bytes are accepted`() = runTest {
        val error = runCatching { store.saveProcessed(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())) }
        assertTrue(error.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `references outside the image directory are rejected`() = runTest {
        File(tmp.root, "files").mkdirs()
        File(tmp.root, "files/secrets.preferences_pb").writeText("x")
        listOf(
            "../secrets.preferences_pb",
            "meal-images/../secrets.preferences_pb",
            "meal-images/../../files/secrets.preferences_pb",
            "/etc/passwd",
            "meal-images/not-a-uuid.jpg",
        ).forEach { ref ->
            assertNull(ref, store.fileFor(ref))
            store.delete(ref)
        }
        assertTrue(File(tmp.root, "files/secrets.preferences_pb").exists())
    }

    @Test
    fun `raw captures are temporary files in the cache`() = runTest {
        val capture = store.newCaptureFile()
        assertEquals(dirs.captures.canonicalFile, capture.parentFile!!.canonicalFile)
        capture.writeBytes(TEST_JPEG)
        store.deleteCapture(capture)
        assertFalse(capture.exists())
    }

    @Test
    fun `deleteCapture refuses files that are not captures`() = runTest {
        val ref = store.saveProcessed(TEST_JPEG)
        val error = runCatching { store.deleteCapture(store.fileFor(ref)!!) }
        assertTrue(error.exceptionOrNull() is IllegalArgumentException)
        assertTrue(store.fileFor(ref)!!.exists())
    }
}
