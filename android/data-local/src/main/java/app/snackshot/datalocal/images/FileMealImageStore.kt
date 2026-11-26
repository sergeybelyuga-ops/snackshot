package app.snackshot.datalocal.images

import app.snackshot.domain.meal.MealImageStore
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where images live. Both are app-private: `files/` is excluded from backup by
 * data_extraction_rules.xml, and `cache/` is never backed up.
 */
data class ImageDirectories(val processed: File, val captures: File)

/**
 * Processed photos in `files/meal-images/<uuid>.jpg`; raw captures in `cache/captures/`.
 * A reference is the path relative to `files/`, for example `meal-images/<uuid>.jpg`.
 */
class FileMealImageStore @Inject constructor(private val dirs: ImageDirectories) : MealImageStore {
    override suspend fun newCaptureFile(): File = withContext(Dispatchers.IO) {
        dirs.captures.ensureDirectory()
        File(dirs.captures, "${UUID.randomUUID()}.jpg").also { check(it.createNewFile()) { "Capture file already exists" } }
    }

    override suspend fun deleteCapture(file: File) = withContext(Dispatchers.IO) {
        require(file.parentFile?.canonicalFile == dirs.captures.canonicalFile) { "Not a capture file" }
        file.delete()
        Unit
    }

    override suspend fun saveProcessed(jpeg: ByteArray): String = withContext(Dispatchers.IO) {
        require(jpeg.size >= 4 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte()) { "Not a JPEG" }
        dirs.processed.ensureDirectory()
        val name = "${UUID.randomUUID()}.jpg"
        val tmp = File(dirs.processed, "$name.tmp")
        tmp.writeBytes(jpeg)
        // Rename so a crash never leaves a half-written photo under a valid reference.
        if (!tmp.renameTo(File(dirs.processed, name))) {
            tmp.delete()
            throw IOException("Could not store the processed image")
        }
        refFor(name)
    }

    override fun fileFor(ref: String): File? = nameOf(ref)?.let { File(dirs.processed, it) }?.takeIf { it.isFile }

    override suspend fun delete(ref: String) = withContext(Dispatchers.IO) {
        nameOf(ref)?.let { File(dirs.processed, it).delete() }
        Unit
    }

    companion object {
        const val PROCESSED_DIR = "meal-images"
        private val NAME = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")

        fun refFor(fileName: String) = "$PROCESSED_DIR/$fileName"

        /** The file name in a well-formed reference; anything else (paths, `..`) is rejected. */
        fun nameOf(ref: String): String? = ref.removePrefix("$PROCESSED_DIR/").takeIf {
            ref.startsWith("$PROCESSED_DIR/") &&
                NAME.matches(it)
        }

        private fun File.ensureDirectory() {
            if (!isDirectory && !mkdirs()) throw IOException("Could not create $name")
        }
    }
}
