package app.snackshot.datalocal.db

import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.SyncState
import app.snackshot.domain.meal.SyncStatus
import app.snackshot.domain.nutrition.CalorieRange
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.ZoneOffset

internal fun MealEntry.toEntity() = MealEntity(
    id = id.value,
    source = source.name,
    name = name,
    mealType = mealType.name,
    eatenAt = eatenAt.toEpochMilli(),
    zoneOffsetSeconds = zoneOffset.totalSeconds,
    grams = grams,
    energyKcal = energyKcal,
    proteinGrams = values.proteinGrams,
    carbsGrams = values.carbsGrams,
    fatGrams = values.fatGrams,
    imageRef = imageRef,
    confidence = confidence,
    edit = edit?.name,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun MealEntity.toDomain() = MealEntry(
    id = MealId(id),
    source = MealSource.valueOf(source),
    name = name,
    mealType = MealType.valueOf(mealType),
    eatenAt = Instant.ofEpochMilli(eatenAt),
    zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
    grams = grams,
    values = NutritionValues(energyKcal, proteinGrams, carbsGrams, fatGrams),
    imageRef = imageRef,
    confidence = confidence,
    edit = edit?.let(MealEdit::valueOf),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun SyncState.toEntity(changedAt: Instant) = SyncStateEntity(
    mealId = mealId.value,
    clientRecordId = clientRecordId,
    clientRecordVersion = clientRecordVersion,
    status = status.name,
    healthConnectRecordId = healthConnectRecordId,
    changedAt = changedAt.toEpochMilli(),
)

internal fun SyncStateEntity.toDomain() = SyncState(
    mealId = MealId(mealId),
    clientRecordId = clientRecordId,
    clientRecordVersion = clientRecordVersion,
    status = SyncStatus.valueOf(status),
    healthConnectRecordId = healthConnectRecordId,
)

internal fun NutritionGoals.toEntity() = NutritionGoalsEntity(
    caloriesMin = calories?.minKcal,
    caloriesMax = calories?.maxKcal,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
)

internal fun NutritionGoalsEntity?.toDomain(): NutritionGoals {
    if (this == null) return NutritionGoals.NONE
    val min = caloriesMin
    val max = caloriesMax
    return NutritionGoals(
        calories = if (min != null && max != null) CalorieRange(min, max) else null,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
    )
}
