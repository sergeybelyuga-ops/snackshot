package app.snackshot.datalocal

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/**
 * A Preferences DataStore for JVM tests. Uses Okio storage, whose atomic move replaces the file
 * on Windows too; the File storage the app uses on Android fails on Windows dev PCs from the
 * second write ("Unable to rename"). The repositories under test do not depend on the storage.
 */
internal fun testDataStore(file: File, scope: CoroutineScope): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.toOkioPath() },
    scope = scope,
)
