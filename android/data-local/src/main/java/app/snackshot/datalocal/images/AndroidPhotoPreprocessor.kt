package app.snackshot.datalocal.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import app.snackshot.domain.meal.InvalidPhoto
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.PhotoLimits
import app.snackshot.domain.meal.PhotoPreprocessor
import app.snackshot.domain.meal.PhotoSource
import app.snackshot.domain.meal.PreprocessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WBS 6.2 on Android. [ImageDecoder] applies the EXIF orientation and scales while decoding, so a
 * 12 MP photo is never held at full size. The output is a plain sRGB baseline JPEG without
 * metadata or gain map ([JpegMetadata]). A raw capture is deleted once it has been read; a gallery
 * original is only read.
 */
class AndroidPhotoPreprocessor @Inject constructor(@ApplicationContext private val context: Context, private val images: MealImageStore) :
    PhotoPreprocessor {

    override suspend fun process(source: PhotoSource): PreprocessResult {
        val started = System.nanoTime()
        try {
            val bitmap = when (val decoded = decode(source)) {
                is Decoded.Refused -> return PreprocessResult.Invalid(decoded.reason)
                is Decoded.Image -> decoded.bitmap
            }
            val width = bitmap.width
            val height = bitmap.height
            val jpeg = withContext(Dispatchers.Default) {
                try {
                    JpegMetadata.strip(PhotoLimits.encodeUnderTarget { quality -> bitmap.encode(quality) })
                } finally {
                    bitmap.recycle()
                }
            }
            val ref = images.saveProcessed(jpeg)
            return PreprocessResult.Processed(ref, jpeg, width, height, (System.nanoTime() - started) / 1_000_000)
        } finally {
            if (source is PhotoSource.Capture) images.deleteCapture(source.file)
        }
    }

    private sealed interface Decoded {
        class Image(val bitmap: Bitmap) : Decoded

        class Refused(val reason: InvalidPhoto) : Decoded
    }

    private class Refusal(val reason: InvalidPhoto) : RuntimeException()

    private suspend fun decode(source: PhotoSource): Decoded = withContext(Dispatchers.IO) {
        try {
            val decoderSource = when (source) {
                is PhotoSource.Capture -> ImageDecoder.createSource(source.file)
                is PhotoSource.Picked -> ImageDecoder.createSource(context.contentResolver, Uri.parse(source.uri))
            }
            val bytes = sourceBytes(source)
            val bitmap = ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
                PhotoLimits.check(info.size.width, info.size.height, bytes)?.let { throw Refusal(it) }
                val (width, height) = PhotoLimits.targetSize(info.size.width, info.size.height)
                decoder.setTargetSize(width, height)
                // A software, mutable bitmap: it is encoded on the CPU, and its gain map can be dropped.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && bitmap.hasGainmap()) bitmap.gainmap = null
            Decoded.Image(bitmap)
        } catch (e: Refusal) {
            Decoded.Refused(e.reason)
        } catch (e: IOException) {
            Decoded.Refused(InvalidPhoto.UNREADABLE)
        } catch (e: IllegalArgumentException) {
            // Unknown URI scheme or an unsupported image.
            Decoded.Refused(InvalidPhoto.UNREADABLE)
        } catch (e: SecurityException) {
            // The picker grant was lost, for example after the process was killed.
            Decoded.Refused(InvalidPhoto.UNREADABLE)
        }
    }

    /** The source's size in bytes, when it is known. */
    private fun sourceBytes(source: PhotoSource): Long? = when (source) {
        is PhotoSource.Capture -> source.file.length()
        is PhotoSource.Picked -> runCatching {
            context.contentResolver.query(Uri.parse(source.uri), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
    }

    private fun Bitmap.encode(quality: Int): ByteArray = ByteArrayOutputStream().also { out ->
        check(compress(Bitmap.CompressFormat.JPEG, quality, out)) { "JPEG encoding failed" }
    }.toByteArray()
}
