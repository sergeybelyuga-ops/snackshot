package app.snackshot.domain.meal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SyncStateTest {
    private val meal = MealId("meal-1")

    @Test
    fun `new meal waits for its first upload with version 1`() {
        val state = SyncState.newMeal(meal)
        assertEquals("snackshot-meal-meal-1", state.clientRecordId)
        assertEquals(1, state.clientRecordVersion)
        assertEquals(SyncStatus.PENDING_UPSERT, state.status)
    }

    @Test
    fun `upload marks it synced and an edit raises the version`() {
        val synced = SyncState.newMeal(meal).uploaded(uploadedVersion = 1, recordId = "hc-1")
        assertEquals(SyncStatus.SYNCED, synced.status)
        assertEquals("hc-1", synced.healthConnectRecordId)

        val edited = synced.edited()
        assertEquals(2, edited.clientRecordVersion)
        assertEquals(SyncStatus.PENDING_UPSERT, edited.status)
        assertEquals(synced.clientRecordId, edited.clientRecordId)
    }

    @Test
    fun `an edit during the upload keeps the meal pending`() {
        val uploading = SyncState.newMeal(meal)
        val editedMeanwhile = uploading.edited()
        val after = editedMeanwhile.uploaded(uploadedVersion = 1, recordId = "hc-1")
        assertEquals(SyncStatus.PENDING_UPSERT, after.status)
        assertEquals(2, after.clientRecordVersion)
    }

    @Test
    fun `delete needs a remote delete only after the meal reached Health Connect`() {
        assertNull(SyncState.newMeal(meal).deleted())
        val synced = SyncState.newMeal(meal).uploaded(1, "hc-1")
        assertEquals(SyncStatus.PENDING_DELETE, synced.deleted()!!.status)
    }

    @Test
    fun `an upload finishing after a delete keeps the delete pending`() {
        val deleting = SyncState.newMeal(meal).uploaded(1, "hc-1").edited().deleted()!!
        assertEquals(SyncStatus.PENDING_DELETE, deleting.uploaded(2, "hc-1").status)
    }
}
