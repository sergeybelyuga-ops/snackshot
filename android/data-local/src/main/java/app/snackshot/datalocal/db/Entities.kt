package app.snackshot.datalocal.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Column rules: times are epoch milliseconds (UTC); enums are stored by name as TEXT and mapped
// explicitly in Mappers.kt, so renaming a Kotlin constant needs a migration, never a silent change.

/** One saved meal. Values are for the whole portion: kcal and grams (doc 02 §4.1). */
@Entity(tableName = "meals", indices = [Index("eaten_at")])
data class MealEntity(
    @PrimaryKey val id: String,
    val source: String,
    val name: String,
    @ColumnInfo(name = "meal_type") val mealType: String,
    @ColumnInfo(name = "eaten_at") val eatenAt: Long,
    @ColumnInfo(name = "zone_offset_seconds") val zoneOffsetSeconds: Int,
    val grams: Double?,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinGrams: Double?,
    @ColumnInfo(name = "carbs_g") val carbsGrams: Double?,
    @ColumnInfo(name = "fat_g") val fatGrams: Double?,
    @ColumnInfo(name = "image_ref") val imageRef: String?,
    val confidence: Double?,
    val edit: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * Health Connect sync metadata, apart from the meal values (doc 02 §4.4). No foreign key: a
 * pending remote delete outlives its meal row.
 */
@Entity(
    tableName = "sync_state",
    indices = [Index("status"), Index(value = ["client_record_id"], unique = true)],
)
data class SyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "meal_id") val mealId: String,
    @ColumnInfo(name = "client_record_id") val clientRecordId: String,
    @ColumnInfo(name = "client_record_version") val clientRecordVersion: Long,
    val status: String,
    @ColumnInfo(name = "health_connect_record_id") val healthConnectRecordId: String?,
    /** When the status last changed; pending work is taken oldest first. */
    @ColumnInfo(name = "changed_at") val changedAt: Long,
)

/** The optional local goals: a single row with id [ID]. */
@Entity(tableName = "nutrition_goals")
data class NutritionGoalsEntity(
    @PrimaryKey val id: Int = ID,
    @ColumnInfo(name = "calories_min") val caloriesMin: Int?,
    @ColumnInfo(name = "calories_max") val caloriesMax: Int?,
    @ColumnInfo(name = "protein_g") val proteinGrams: Int?,
    @ColumnInfo(name = "carbs_g") val carbsGrams: Int?,
    @ColumnInfo(name = "fat_g") val fatGrams: Int?,
) {
    companion object {
        const val ID = 1
    }
}

/** A product metric counter for one local day (ISO date). */
@Entity(tableName = "metric_counters", primaryKeys = ["day", "counter"])
data class MetricCounterEntity(val day: String, val counter: String, val value: Long)

/** One duration measurement for one local day, for p50 / p95. */
@Entity(tableName = "metric_samples", indices = [Index("day")])
data class MetricSampleEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val day: String, val sample: String, val value: Long)
