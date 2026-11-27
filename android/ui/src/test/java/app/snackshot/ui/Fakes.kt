package app.snackshot.ui

import app.snackshot.domain.access.AccessCheck
import app.snackshot.domain.access.AiAccess
import app.snackshot.domain.access.AiAccessRepository
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.PictureAllowance
import app.snackshot.domain.meal.MealEdit
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealId
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.MealRepository
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.nutrition.GoalsRepository
import app.snackshot.domain.nutrition.NutritionGoals
import app.snackshot.domain.nutrition.NutritionValues
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

val TEST_ZONE: ZoneId = ZoneId.of("Europe/Belgrade")

/** 27 Nov 2026, 13:00 in Belgrade. */
val TEST_NOW: Instant = Instant.parse("2026-11-27T12:00:00Z")

class MutableClock(var now: Instant = TEST_NOW) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = object : Clock() {
        override fun instant() = now

        override fun getZone() = zone

        override fun withZone(zone: ZoneId?) = this@MutableClock.withZone(zone)
    }
}

class FakeMealRepository(initial: List<MealEntry> = emptyList()) : MealRepository {
    val meals = MutableStateFlow(initial)

    override fun observeMeals(from: Instant, until: Instant): Flow<List<MealEntry>> =
        meals.map { list -> list.filter { !it.eatenAt.isBefore(from) && it.eatenAt.isBefore(until) }.sortedByDescending { it.eatenAt } }

    override fun observeMeal(id: MealId): Flow<MealEntry?> = meals.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getMeal(id: MealId): MealEntry? = meals.value.firstOrNull { it.id == id }

    override suspend fun save(entry: MealEntry) {
        meals.value = meals.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun delete(id: MealId) {
        meals.value = meals.value.filterNot { it.id == id }
    }
}

class FakeGoals(initial: NutritionGoals = NutritionGoals.NONE) : GoalsRepository {
    val state = MutableStateFlow(initial)
    override val goals: Flow<NutritionGoals> = state

    override suspend fun setGoals(goals: NutritionGoals) {
        state.value = goals
    }
}

class FakeAccess : AiAccessRepository {
    val state = MutableStateFlow<AiAccess?>(null)
    var next: AccessCheck = AccessCheck.Available(access())

    /** When set, ensureAccess waits for it (to test a slow broker). */
    var gate: CompletableDeferred<Unit>? = null
    var ensureCalls = 0
    var refreshCalls = 0

    override val access: Flow<AiAccess?> = state

    override suspend fun ensureAccess(): AccessCheck {
        ensureCalls++
        gate?.await()
        return next
    }

    override suspend fun refreshIfStale(): AccessCheck? {
        refreshCalls++
        return null
    }

    val reported = mutableListOf<String>()

    override suspend fun reportPicture(requestId: String): AccessCheck {
        reported += requestId
        return next
    }

    override suspend fun updateProfile(language: String, country: String) = true

    override suspend fun deleteAccount() = true

    companion object {
        fun access(status: AiAccessStatus = AiAccessStatus.TRIAL, used: Int = 0, aiEnabled: Boolean = true) =
            AiAccess(status, null, PictureAllowance(used, 90), isTest = true, aiEnabled = aiEnabled, checkedAt = TEST_NOW)
    }
}

fun quickAddMeal(
    id: String,
    eatenAt: Instant,
    kcal: Double,
    name: String = "Meal $id",
    protein: Double? = null,
    carbs: Double? = null,
    fat: Double? = null,
) = MealEntry(
    id = MealId(id),
    source = MealSource.QUICK_ADD,
    name = name,
    mealType = MealType.LUNCH,
    eatenAt = eatenAt,
    zoneOffset = TEST_ZONE.rules.getOffset(eatenAt),
    grams = null,
    values = NutritionValues(kcal, protein, carbs, fat),
    imageRef = null,
    confidence = null,
    edit = null,
    createdAt = eatenAt,
    updatedAt = eatenAt,
)

/** An AI result, for rows that show a photo source. */
fun photoMeal(id: String, eatenAt: Instant, kcal: Double) = quickAddMeal(id, eatenAt, kcal).copy(
    source = MealSource.PHOTO,
    grams = 250.0,
    values = NutritionValues(kcal, 10.0, 20.0, 5.0),
    confidence = 0.9,
    edit = MealEdit.NONE,
)

/** The pending meal flow in memory; [discarded] and [completed] count the endings. */
class FakePendingMeals(initial: PendingMeal? = null) : PendingMealStore {
    val state = MutableStateFlow(initial)
    var completed = 0
    var discarded = 0

    override val pending: Flow<PendingMeal?> = state

    override suspend fun save(meal: PendingMeal) {
        state.value = meal
    }

    override suspend fun complete() {
        completed++
        state.value = null
    }

    override suspend fun discard() {
        discarded++
        state.value = null
    }
}

/** Processed photos in a temporary folder. */
class FakeImages(private val dir: java.io.File) : MealImageStore {
    override suspend fun newCaptureFile(): java.io.File = java.io.File.createTempFile("capture", ".jpg", dir)

    override suspend fun deleteCapture(file: java.io.File) {
        file.delete()
    }

    override suspend fun saveProcessed(jpeg: ByteArray): String {
        val name = "meal-images/${java.util.UUID.randomUUID()}.jpg"
        java.io.File(dir, name).apply { parentFile?.mkdirs() }.writeBytes(jpeg)
        return name
    }

    override fun fileFor(ref: String): java.io.File? = java.io.File(dir, ref).takeIf { it.isFile }

    override suspend fun delete(ref: String) {
        java.io.File(dir, ref).delete()
    }
}
