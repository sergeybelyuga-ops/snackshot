package app.snackshot.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.access.AiAccessRepository
import app.snackshot.domain.access.AiAccessStatus
import app.snackshot.domain.access.PictureAllowance
import app.snackshot.domain.meal.MealImageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** S-03 camera (doc 10 §4.5): where the raw capture goes, and the free-trial counter. */
@HiltViewModel
class CameraViewModel @Inject constructor(private val images: MealImageStore, access: AiAccessRepository) : ViewModel() {
    /** "N of 90 free pictures left" while the trial runs; null otherwise. */
    val trial: StateFlow<PictureAllowance?> = access.access
        .map { a -> a?.takeIf { it.status == AiAccessStatus.TRIAL }?.pictures }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A new file in the app's cache for one capture. */
    suspend fun newCaptureFile(): File = images.newCaptureFile()

    /** A capture that failed or was abandoned. */
    fun discardCapture(file: File) {
        viewModelScope.launch { images.deleteCapture(file) }
    }
}
