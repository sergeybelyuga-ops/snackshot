package app.snackshot.datalocal.images

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpegMetadataTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private val soi = bytes(0xFF, 0xD8)
    private val app0 = bytes(0xFF, 0xE0, 0x00, 0x04, 'J'.code, 'F'.code)
    private val exif = bytes(0xFF, 0xE1, 0x00, 0x06, 'E'.code, 'x'.code, 'i'.code, 'f'.code)
    private val icc = bytes(0xFF, 0xE2, 0x00, 0x03, 0x42)
    private val dqt = bytes(0xFF, 0xDB, 0x00, 0x03, 0x07)

    // Scan data with a stuffed 0xFF 0x00 and a restart marker, then the end of image.
    private val scan = bytes(0xFF, 0xDA, 0x00, 0x02, 0x11, 0xFF, 0x00, 0x22, 0xFF, 0xD0, 0x33, 0xFF, 0xD9)
    private val gainMap = bytes(0xFF, 0xD8, 0xFF, 0xE1, 0x00, 0x02, 0xFF, 0xD9)

    @Test
    fun `APP1 to APP15 and a trailing gain map are removed, everything else kept`() {
        val input = soi + app0 + exif + icc + dqt + scan + gainMap

        assertArrayEquals(soi + app0 + dqt + scan, JpegMetadata.strip(input))
    }

    @Test
    fun `a clean JPEG is unchanged`() {
        val input = soi + app0 + dqt + scan

        assertArrayEquals(input, JpegMetadata.strip(input))
    }

    @Test
    fun `malformed input is returned as is`() {
        val truncated = soi + bytes(0xFF, 0xE1, 0x00, 0x40, 0x01)
        val notJpeg = bytes(0x89, 'P'.code, 'N'.code, 'G'.code)

        assertArrayEquals(truncated, JpegMetadata.strip(truncated))
        assertArrayEquals(notJpeg, JpegMetadata.strip(notJpeg))
    }
}
