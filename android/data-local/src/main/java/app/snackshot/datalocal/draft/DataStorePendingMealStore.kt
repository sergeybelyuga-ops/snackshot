package app.snackshot.datalocal.draft

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.snackshot.datalocal.db.SnackshotDatabase
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The `pending_meal` DataStore: one JSON document, the unfinished meal flow. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PendingMealDataStore

/**
 * The unfinished meal flow on disk (doc 02 §1.5): written on every change, so it survives
 * backgrounding, process death and a force stop. An image is deleted here only when no saved
 * meal uses it, so discarding an edit of a saved meal keeps that meal's photo.
 */
class DataStorePendingMealStore @Inject constructor(
    @PendingMealDataStore private val store: DataStore<Preferences>,
    private val images: MealImageStore,
    private val db: SnackshotDatabase,
) : PendingMealStore {
    override val pending: Flow<PendingMeal?> = store.data.map { prefs -> prefs[KEY]?.let(PendingMealJson::decode) }.distinctUntilChanged()

    override suspend fun save(meal: PendingMeal) {
        val previous = pending.first()
        store.edit { it[KEY] = PendingMealJson.encode(meal) }
        // A retake replaces the photo: the old one is no longer needed.
        previous?.imageRef?.takeIf { it != meal.imageRef }?.let { deleteIfUnused(it) }
    }

    override suspend fun complete() {
        store.edit { it.remove(KEY) }
    }

    override suspend fun discard() {
        val previous = pending.first()
        store.edit { it.remove(KEY) }
        previous?.imageRef?.let { deleteIfUnused(it) }
    }

    private suspend fun deleteIfUnused(ref: String) {
        if (db.meals().countWithImage(ref) == 0) images.delete(ref)
    }

    private companion object {
        val KEY = stringPreferencesKey("pending_meal")
    }
}
