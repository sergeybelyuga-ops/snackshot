package app.snackshot.domain.meal

import java.io.File

/** Where a meal photo comes from (S-03 camera, S-04 gallery). */
sealed interface PhotoSource {
    /** A raw camera capture in [MealImageStore.newCaptureFile]; deleted once it is processed. */
    data class Capture(val file: File) : PhotoSource

    /** A Photo Picker item (`content://`). The original is only read, never copied or changed. */
    data class Picked(val uri: String) : PhotoSource
}

/** Why a photo can't be used. S-03/S-04 explain it and offer another photo or Quick add. */
enum class InvalidPhoto {
    /** Not an image, an unsupported format, or a file that can't be read. */
    UNREADABLE,

    /** Smaller than [PhotoLimits.MIN_SIDE_PX] on its short side: too little detail to recognize. */
    TOO_SMALL,

    /** Larger than [PhotoLimits.MAX_SOURCE_BYTES]. */
    TOO_LARGE,
}

sealed interface PreprocessResult {
    /**
     * The processed photo is stored under [imageRef]; [jpeg] is what is sent for recognition.
     * [millis] is the time it took, for the latency measurement (I-4).
     */
    class Processed(val imageRef: String, val jpeg: ByteArray, val width: Int, val height: Int, val millis: Long) : PreprocessResult

    data class Invalid(val reason: InvalidPhoto) : PreprocessResult
}

/**
 * WBS 6.2: decode with the EXIF orientation applied, scale the long side down to
 * [PhotoLimits.MAX_SIDE_PX], drop metadata and the Ultra HDR gain map, and encode a plain JPEG
 * under [PhotoLimits.TARGET_BYTES] where practical (doc 10 §4.5, spike §7).
 */
interface PhotoPreprocessor {
    suspend fun process(source: PhotoSource): PreprocessResult
}

object PhotoLimits {
    const val MAX_SIDE_PX = 1000
    const val MIN_SIDE_PX = 200
    const val TARGET_BYTES = 1_000_000
    const val MAX_SOURCE_BYTES = 50_000_000L

    /** JPEG qualities tried in order until the photo is under [TARGET_BYTES]; the last one is kept regardless. */
    val QUALITIES = listOf(85, 75, 65, 55)

    /** The decoded size: the long side at most [maxSide], the aspect ratio kept, never upscaled. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Pair<Int, Int> {
        require(width > 0 && height > 0) { "Image size must be positive: $width × $height" }
        val long = maxOf(width, height)
        if (long <= maxSide) return width to height
        val scale = maxSide.toDouble() / long
        return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
    }

    fun check(width: Int, height: Int, sourceBytes: Long?): InvalidPhoto? = when {
        sourceBytes != null && sourceBytes > MAX_SOURCE_BYTES -> InvalidPhoto.TOO_LARGE
        minOf(width, height) < MIN_SIDE_PX -> InvalidPhoto.TOO_SMALL
        else -> null
    }

    /** Encodes at each of [QUALITIES] until one fits [TARGET_BYTES]. */
    inline fun encodeUnderTarget(encode: (quality: Int) -> ByteArray): ByteArray {
        var last = ByteArray(0)
        for (quality in QUALITIES) {
            last = encode(quality)
            if (last.size < TARGET_BYTES) return last
        }
        return last
    }
}
