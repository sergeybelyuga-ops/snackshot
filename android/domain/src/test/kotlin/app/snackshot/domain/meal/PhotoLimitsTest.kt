package app.snackshot.domain.meal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PhotoLimitsTest {
    @Test
    fun `the long side is scaled to 1000 px and the aspect ratio kept`() {
        assertEquals(1000 to 750, PhotoLimits.targetSize(4000, 3000))
        assertEquals(563 to 1000, PhotoLimits.targetSize(2252, 4000))
    }

    @Test
    fun `small photos are never upscaled`() {
        assertEquals(800 to 600, PhotoLimits.targetSize(800, 600))
    }

    @Test
    fun `too small or too large sources are refused`() {
        assertEquals(InvalidPhoto.TOO_SMALL, PhotoLimits.check(1200, 150, 50_000))
        assertEquals(InvalidPhoto.TOO_LARGE, PhotoLimits.check(4000, 3000, 60_000_000))
        assertNull(PhotoLimits.check(4000, 3000, null))
    }

    @Test
    fun `quality steps down until the JPEG is under the target`() {
        val tried = mutableListOf<Int>()
        val result = PhotoLimits.encodeUnderTarget { q ->
            tried += q
            ByteArray(if (q > 70) 1_200_000 else 900_000)
        }
        assertEquals(listOf(85, 75, 65), tried)
        assertEquals(900_000, result.size)
    }

    @Test
    fun `the lowest quality is kept when nothing fits`() {
        val result = PhotoLimits.encodeUnderTarget { ByteArray(2_000_000) }
        assertEquals(2_000_000, result.size)
    }
}
