package app.snackshot.datalocal.draft

import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.Portion
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON form of [PendingMeal]. Kept apart from the domain types so they stay free of
 * serialization. [SCHEMA] goes up on an incompatible change; an older or unreadable draft is
 * dropped, which costs the user one unfinished entry, never a saved meal.
 */
internal object PendingMealJson {
    const val SCHEMA = 1

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
    }

    fun encode(meal: PendingMeal): String = json.encodeToString(Envelope.serializer(), Envelope(SCHEMA, meal.toDto()))

    /** Null when the text is not a readable draft of this [SCHEMA]. */
    fun decode(text: String): PendingMeal? = runCatching {
        val envelope = json.decodeFromString(Envelope.serializer(), text)
        if (envelope.schema != SCHEMA) null else envelope.meal.toDomain()
    }.getOrNull()

    @Serializable
    private data class Envelope(val schema: Int, val meal: PendingMealDto)

    @Serializable
    private sealed interface PendingMealDto

    @Serializable
    @SerialName("awaiting_recognition")
    private data class AwaitingDto(
        val source: String,
        val imageRef: String,
        val capturedAt: Long,
        val zoneOffsetSeconds: Int,
        val failedAttempts: Int,
    ) : PendingMealDto

    @Serializable
    @SerialName("reviewing")
    private data class ReviewingDto(val draft: DraftDto) : PendingMealDto

    @Serializable
    private data class ValuesDto(
        val energyKcal: Double? = null,
        val proteinGrams: Double? = null,
        val carbsGrams: Double? = null,
        val fatGrams: Double? = null,
    )

    @Serializable
    private data class PortionDto(val grams: Double, val values: ValuesDto)

    @Serializable
    private data class DraftDto(
        val source: String,
        val eatenAt: Long,
        val zoneOffsetSeconds: Int,
        val mealType: String,
        val name: String,
        val grams: Double?,
        val basis: PortionDto?,
        val overrides: ValuesDto,
        val imageRef: String?,
        val confidence: Double?,
        val originalName: String,
        val editing: EntryDto?,
    )

    @Serializable
    private data class EntryDto(
        val id: String,
        val source: String,
        val name: String,
        val mealType: String,
        val eatenAt: Long,
        val zoneOffsetSeconds: Int,
        val grams: Double?,
        val values: ValuesDto,
        val imageRef: String?,
        val confidence: Double?,
        val edit: String?,
        val createdAt: Long,
        val updatedAt: Long,
    )

    private fun PendingMeal.toDto(): PendingMealDto = when (this) {
        is PendingMeal.AwaitingRecognition ->
            AwaitingDto(source.name, imageRef, capturedAt.toEpochMilli(), zoneOffset.totalSeconds, failedAttempts)
        is PendingMeal.Reviewing -> ReviewingDto(draft.toDto())
    }

    private fun PendingMealDto.toDomain(): PendingMeal = when (this) {
        is AwaitingDto -> PendingMeal.AwaitingRecognition(
            source = MealSource.valueOf(source),
            imageRef = imageRef,
            capturedAt = Instant.ofEpochMilli(capturedAt),
            zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
            failedAttempts = failedAttempts,
        )
        is ReviewingDto -> PendingMeal.Reviewing(draft.toDomain())
    }

    private fun NutritionValues.toDto() = ValuesDto(energyKcal, proteinGrams, carbsGrams, fatGrams)

    private fun ValuesDto.toDomain() = NutritionValues(energyKcal, proteinGrams, carbsGrams, fatGrams)

    private fun MealDraft.toDto() = DraftDto(
        source = source.name,
        eatenAt = eatenAt.toEpochMilli(),
        zoneOffsetSeconds = zoneOffset.totalSeconds,
        mealType = mealType.name,
        name = name,
        grams = grams,
        basis = basis?.let { PortionDto(it.grams, it.values.toDto()) },
        overrides = overrides.toDto(),
        imageRef = imageRef,
        confidence = confidence,
        originalName = originalName,
        editing = editing?.toDto(),
    )

    private fun DraftDto.toDomain() = MealDraft(
        source = MealSource.valueOf(source),
        eatenAt = Instant.ofEpochMilli(eatenAt),
        zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
        mealType = MealType.valueOf(mealType),
        name = name,
        grams = grams,
        basis = basis?.let { Portion(it.grams, it.values.toDomain()) },
        overrides = overrides.toDomain(),
        imageRef = imageRef,
        confidence = confidence,
        originalName = originalName,
        editing = editing?.toDomain(),
    )

    private fun MealEntry.toDto() = EntryDto(
        id = id.value,
        source = source.name,
        name = name,
        mealType = mealType.name,
        eatenAt = eatenAt.toEpochMilli(),
        zoneOffsetSeconds = zoneOffset.totalSeconds,
        grams = grams,
        values = values.toDto(),
        imageRef = imageRef,
        confidence = confidence,
        edit = edit?.name,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

    private fun EntryDto.toDomain() = MealEntry(
        id = MealId(id),
        source = MealSource.valueOf(source),
        name = name,
        mealType = MealType.valueOf(mealType),
        eatenAt = Instant.ofEpochMilli(eatenAt),
        zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
        grams = grams,
        values = values.toDomain(),
        imageRef = imageRef,
        confidence = confidence,
        edit = edit?.let(MealEdit::valueOf),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}
