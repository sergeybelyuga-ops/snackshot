package app.snackshot.datalocal.images

import java.io.ByteArrayOutputStream

/**
 * Removes what a re-encoded JPEG may still carry from its source (spike §7): the `APP1`–`APP15`
 * segments (EXIF, XMP, ICC, the MPF index of an Ultra HDR photo) and anything after the first end
 * of image (the Ultra HDR gain-map JPEG). `APP0` (JFIF) and the image data are kept.
 *
 * A malformed file is returned unchanged; the preprocessor only feeds it its own encoder output.
 */
internal object JpegMetadata {
    fun strip(jpeg: ByteArray): ByteArray {
        if (jpeg.size < 4 || jpeg.u(0) != 0xFF || jpeg.u(1) != SOI) return jpeg
        val out = ByteArrayOutputStream(jpeg.size)
        out.write(jpeg, 0, 2)
        var i = 2
        while (i + 4 <= jpeg.size) {
            if (jpeg.u(i) != 0xFF) return jpeg
            val marker = jpeg.u(i + 1)
            when {
                marker == 0xFF -> {
                    // Fill byte before a marker.
                    i++
                    continue
                }
                marker == SOS -> {
                    val end = endOfImage(jpeg, i) ?: return jpeg
                    out.write(jpeg, i, end - i)
                    return out.toByteArray()
                }
                marker in RST0..RST7 || marker == TEM -> {
                    out.write(jpeg, i, 2)
                    i += 2
                    continue
                }
            }
            val length = (jpeg.u(i + 2) shl 8) or jpeg.u(i + 3)
            val next = i + 2 + length
            if (length < 2 || next > jpeg.size) return jpeg
            if (marker !in APP1..APP15) out.write(jpeg, i, next - i)
            i = next
        }
        return jpeg
    }

    /** The index just after the first EOI from [from]. Inside scan data a 0xFF is followed by 0x00 or a restart marker. */
    private fun endOfImage(jpeg: ByteArray, from: Int): Int? {
        var i = from
        while (i + 1 < jpeg.size) {
            if (jpeg.u(i) == 0xFF && jpeg.u(i + 1) == EOI) return i + 2
            i++
        }
        return null
    }

    private fun ByteArray.u(i: Int) = this[i].toInt() and 0xFF

    private const val SOI = 0xD8
    private const val EOI = 0xD9
    private const val SOS = 0xDA
    private const val TEM = 0x01
    private const val RST0 = 0xD0
    private const val RST7 = 0xD7
    private const val APP1 = 0xE1
    private const val APP15 = 0xEF
}
