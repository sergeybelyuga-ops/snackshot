package app.snackshot.logmeal

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Proves the shared test-fixtures folder is on the test classpath. */
class FixturesTest {
    private fun fixture(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
        assertNotNull(stream, "missing fixture $path")
        return stream!!.bufferedReader().use { it.readText() }
    }

    @Test
    fun `segmentation fixture is scrubbed`() {
        val json = fixture("logmeal/segmentation-complete/ok-single-dish-apple.json")
        assertTrue(json.contains("\"imageId\": 1000001"), "imageId must be the fake scrubbed value")
        assertTrue(json.contains("\"apple\""))
    }

    @Test
    fun `sample images are available`() {
        assertNotNull(javaClass.classLoader.getResource("images/apple.jpg"))
    }
}
