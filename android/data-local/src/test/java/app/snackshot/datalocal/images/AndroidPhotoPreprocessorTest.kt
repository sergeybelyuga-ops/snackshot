package app.snackshot.datalocal.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.snackshot.domain.meal.InvalidPhoto
import app.snackshot.domain.meal.PhotoLimits
import app.snackshot.domain.meal.PhotoSource
import app.snackshot.domain.meal.PreprocessResult
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Real decoding with Robolectric's native graphics. Excluded on Windows, where Smart App Control can
 * block the native library (data-local/build.gradle.kts); CI runs it on Linux.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidPhotoPreprocessorTest {
    @get:Rule val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dirs by lazy {
        ImageDirectories(processed = File(tmp.root, "files/meal-images"), captures = File(tmp.root, "cache/captures"))
    }
    private val store by lazy { FileMealImageStore(dirs) }
    private val preprocessor by lazy { AndroidPhotoPreprocessor(context, store) }

    private fun jpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(200, 60, 40))
            // Some detail, so the encoder has work to do.
            val paint = android.graphics.Paint().apply { color = Color.rgb(30, 140, 60) }
            for (x in 0 until width step 40) drawRect(x.toFloat(), 0f, x + 20f, height.toFloat(), paint)
        }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
    }

    private suspend fun capture(bytes: ByteArray): File = store.newCaptureFile().also { it.writeBytes(bytes) }

    private fun processed(result: PreprocessResult): PreprocessResult.Processed {
        assertTrue("expected Processed, got $result", result is PreprocessResult.Processed)
        return result as PreprocessResult.Processed
    }

    @Test
    fun `a large capture is scaled to 1000 px, stored and deleted`() = runTest {
        val raw = capture(jpeg(3000, 2000))

        val result = processed(preprocessor.process(PhotoSource.Capture(raw)))

        assertEquals(1000, result.width)
        assertEquals(667, result.height)
        assertTrue(result.jpeg.size < PhotoLimits.TARGET_BYTES)
        assertEquals(0xFF.toByte(), result.jpeg[0])
        assertEquals(0xD8.toByte(), result.jpeg[1])
        assertArrayEquals(result.jpeg, store.fileFor(result.imageRef)!!.readBytes())
        val decoded = BitmapFactory.decodeByteArray(result.jpeg, 0, result.jpeg.size)
        assertEquals(1000, decoded.width)
        assertFalse("the raw capture is deleted", raw.exists())
    }

    @Test
    fun `a picked photo is read and left untouched`() = runTest {
        val original = File(tmp.root, "gallery.jpg").apply { writeBytes(jpeg(1200, 1600)) }
        val before = original.readBytes()

        val result = processed(preprocessor.process(PhotoSource.Picked(Uri.fromFile(original).toString())))

        assertEquals(750, result.width)
        assertEquals(1000, result.height)
        assertArrayEquals(before, original.readBytes())
    }

    @Test
    fun `a photo already small enough keeps its size`() = runTest {
        val result = processed(preprocessor.process(PhotoSource.Capture(capture(jpeg(800, 600)))))

        assertEquals(800, result.width)
        assertEquals(600, result.height)
    }

    @Test
    fun `a tiny image is refused and its capture deleted`() = runTest {
        val raw = capture(jpeg(150, 150))

        assertEquals(PreprocessResult.Invalid(InvalidPhoto.TOO_SMALL), preprocessor.process(PhotoSource.Capture(raw)))
        assertFalse(raw.exists())
        assertTrue(dirs.processed.listFiles().isNullOrEmpty())
    }

    @Test
    fun `something that is not an image is unreadable`() = runTest {
        val raw = capture("not a photo".toByteArray())

        assertEquals(PreprocessResult.Invalid(InvalidPhoto.UNREADABLE), preprocessor.process(PhotoSource.Capture(raw)))
        assertFalse(raw.exists())
    }

    @Test
    fun `a missing picker item is unreadable`() = runTest {
        val gone = Uri.fromFile(File(tmp.root, "deleted.jpg")).toString()

        assertEquals(PreprocessResult.Invalid(InvalidPhoto.UNREADABLE), preprocessor.process(PhotoSource.Picked(gone)))
    }
}
