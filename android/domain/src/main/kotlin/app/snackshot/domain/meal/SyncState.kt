package app.snackshot.domain.meal

/** Where a meal stands against Health Connect (F-20, WBS 8.2). */
enum class SyncStatus {
    /** Created or edited locally; the next sync upserts it. */
    PENDING_UPSERT,

    /** Deleted locally; the next sync deletes the Health Connect record, then the metadata goes. */
    PENDING_DELETE,

    /** Health Connect has the current version. */
    SYNCED,
}

/**
 * Health Connect sync metadata for one meal, stored apart from the meal values (doc 02 §4.4).
 * [clientRecordVersion] grows with every local change, so Health Connect accepts an upsert
 * only when it is newer than what it holds (doc 02 §4.8).
 */
data class SyncState(
    val mealId: MealId,
    val clientRecordId: String,
    val clientRecordVersion: Long,
    val status: SyncStatus,
    val healthConnectRecordId: String? = null,
) {
    init {
        require(clientRecordVersion >= 1) { "clientRecordVersion starts at 1" }
    }

    /** After a local edit: a new version to upload. */
    fun edited(): SyncState = copy(clientRecordVersion = clientRecordVersion + 1, status = SyncStatus.PENDING_UPSERT)

    /**
     * After a local delete. A meal that never reached Health Connect needs no remote delete.
     * The sync engine (WBS 8.2) must not delete a meal's metadata while its upload is running.
     */
    fun deleted(): SyncState? = if (healthConnectRecordId == null) null else copy(status = SyncStatus.PENDING_DELETE)

    /**
     * After Health Connect accepted [uploadedVersion]. When the meal was edited while the upload
     * was running, it stays pending so the newer version goes out next time.
     */
    fun uploaded(uploadedVersion: Long, recordId: String): SyncState = copy(
        healthConnectRecordId = recordId,
        status = if (uploadedVersion == clientRecordVersion && status == SyncStatus.PENDING_UPSERT) SyncStatus.SYNCED else status,
    )

    companion object {
        fun newMeal(mealId: MealId) = SyncState(
            mealId = mealId,
            clientRecordId = clientRecordIdFor(mealId),
            clientRecordVersion = 1,
            status = SyncStatus.PENDING_UPSERT,
        )

        /** Stable and unique per meal, so re-uploading the same meal upserts instead of duplicating. */
        fun clientRecordIdFor(mealId: MealId) = "snackshot-meal-${mealId.value}"
    }
}
