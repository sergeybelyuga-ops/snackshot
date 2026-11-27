package app.snackshot.ui.meal

import app.snackshot.domain.access.AiAccessRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reports each successful recognition to the broker as one picture (I-4, doc 11 §6.1a). It runs
 * outside the screen that asked for it, so leaving S-05 for the review never cancels a report.
 * The report is idempotent by its request id. A failed report is not retried (known gap: the
 * picture is then not counted for this client).
 */
@Singleton
class PictureReporter internal constructor(private val access: AiAccessRepository, private val scope: CoroutineScope) {
    @Inject constructor(access: AiAccessRepository) : this(access, CoroutineScope(SupervisorJob() + Dispatchers.IO))

    fun report(requestId: String = UUID.randomUUID().toString()) {
        scope.launch { runCatching { access.reportPicture(requestId) } }
    }
}
