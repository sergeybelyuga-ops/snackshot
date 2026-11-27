package app.snackshot.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size as PixelSize
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snackshot.ui.R
import app.snackshot.ui.theme.Size
import app.snackshot.ui.theme.Spacing
import java.io.File
import kotlinx.coroutines.launch

private enum class CameraPanel { NONE, PERMISSION, DENIED, NO_CAMERA, FAILED }

/**
 * S-03 Camera capture (doc 10 §4.5, WBS 6.1). The permission is asked only here, after a short
 * rationale; without it, or without a camera, the gallery is offered instead. The raw capture
 * goes to the app's cache and is deleted once processed.
 */
@Composable
fun CameraRoute(
    viewModel: CameraViewModel,
    onCaptured: (File) -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trial by viewModel.trial.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var panel by rememberSaveable {
        mutableStateOf(
            when {
                !hasCamera -> CameraPanel.NO_CAMERA
                granted() -> CameraPanel.NONE
                else -> CameraPanel.PERMISSION
            },
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        panel = when {
            ok -> CameraPanel.NONE
            // Denied with "don't ask again" (or twice): only Settings can turn it back on.
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false -> CameraPanel.DENIED
            else -> CameraPanel.PERMISSION
        }
    }
    // Back from Settings with the permission turned on.
    LifecycleResumeEffect(Unit) {
        if ((panel == CameraPanel.DENIED || panel == CameraPanel.PERMISSION) && granted()) panel = CameraPanel.NONE
        onPauseOrDispose { }
    }

    var flash by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    /** The camera is bound; until then the shutter is off and "Starting camera…" shows. */
    var ready by remember { mutableStateOf(false) }
    val capture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // About 2 MP is plenty for a 1000 px upload and keeps capture and decoding fast.
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(PixelSize(1600, 1200), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                    )
                    .build(),
            )
            .build()
    }
    capture.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF

    fun shoot() {
        if (busy || !ready) return
        busy = true
        scope.launch {
            val file = viewModel.newCaptureFile()
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        busy = false
                        onCaptured(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        busy = false
                        viewModel.discardCapture(file)
                        panel = CameraPanel.FAILED
                    }
                },
            )
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (panel == CameraPanel.NONE) {
            CameraPreview(capture, onReady = { ready = true }, onFailed = { panel = CameraPanel.FAILED })
            // Scrims keep the white controls readable over a bright preview (prototype `.preview-scrim`).
            Box(
                Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))),
            )
            Box(
                Modifier.fillMaxWidth().height(220.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))),
            )
        }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.grid, vertical = Spacing.grid),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.camera_close),
                        tint = Color.White,
                    )
                }
                Box(Modifier.weight(1f))
                if (panel == CameraPanel.NONE) {
                    IconButton(onClick = { flash = !flash }) {
                        Icon(
                            painterResource(if (flash) R.drawable.ic_flash_on else R.drawable.ic_flash_off),
                            contentDescription = stringResource(if (flash) R.string.camera_flash_on else R.string.camera_flash_off),
                            tint = Color.White,
                        )
                    }
                }
            }
            if (panel == CameraPanel.NONE) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        Viewfinder(Modifier.padding(bottom = Spacing.medium))
                        if (!ready) ScrimLabel(stringResource(R.string.camera_starting))
                        ScrimLabel(stringResource(R.string.camera_hint))
                        trial?.let {
                            ScrimLabel(stringResource(R.string.camera_trial, it.left, it.limit), icon = R.drawable.ic_photo_library)
                        }
                    }
                }
                ShutterRow(busy = busy, enabled = ready, onShoot = ::shoot, onGallery = onGallery)
            } else {
                PermissionPanel(
                    panel = panel,
                    onAllow = { permission.launch(Manifest.permission.CAMERA) },
                    onSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onRetry = { panel = if (granted()) CameraPanel.NONE else CameraPanel.PERMISSION },
                    onGallery = onGallery,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(capture: ImageCapture, onReady: () -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(owner) {
        try {
            val p = ProcessCameraProvider.awaitInstance(context)
            provider = p
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            p.unbindAll()
            // The back camera for a plate; any camera on a device (or emulator) without one.
            val selector = when {
                p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> throw IllegalStateException("No camera")
            }
            p.bindToLifecycle(owner, selector, preview, capture)
            onReady()
        } catch (e: IllegalArgumentException) {
            // No back camera.
            onFailed()
        } catch (e: IllegalStateException) {
            onFailed()
        } catch (e: androidx.camera.core.CameraUnavailableException) {
            onFailed()
        } catch (e: androidx.camera.core.CameraInfoUnavailableException) {
            onFailed()
        }
    }
    DisposableEffect(Unit) { onDispose { provider?.unbindAll() } }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
}

/** The framing guide: four rounded corners in the primary color. */
@Composable
private fun Viewfinder(modifier: Modifier = Modifier) {
    val color = CameraAccent
    Box(
        modifier.size(VIEWFINDER).drawBehind {
            val stroke = 4.dp.toPx()
            val arm = 32.dp.toPx()
            val r = 12.dp.toPx()
            val w = size.width
            val h = size.height
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                // An L with a rounded joint: two arms meeting at (x, y).
                drawLine(color, Offset(x + dx * r, y), Offset(x + dx * arm, y), stroke, StrokeCap.Round)
                drawLine(color, Offset(x, y + dy * r), Offset(x, y + dy * arm), stroke, StrokeCap.Round)
                val left = if (dx > 0) x else x - 2 * r
                val top = if (dy > 0) y else y - 2 * r
                val start = when {
                    dx > 0 && dy > 0 -> 180f
                    dx < 0 && dy > 0 -> 270f
                    dx < 0 && dy < 0 -> 0f
                    else -> 90f
                }
                drawArc(
                    color,
                    start,
                    90f,
                    false,
                    Offset(left, top),
                    androidx.compose.ui.geometry.Size(2 * r, 2 * r),
                    style = Stroke(stroke),
                )
            }
            corner(0f, 0f, 1f, 1f)
            corner(w, 0f, -1f, 1f)
            corner(0f, h, 1f, -1f)
            corner(w, h, -1f, -1f)
        },
    )
}

@Composable
private fun ScrimLabel(text: String, @DrawableRes icon: Int? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = Spacing.medium, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun ShutterRow(busy: Boolean, enabled: Boolean, onShoot: () -> Unit, onGallery: () -> Unit) {
    val take = stringResource(R.string.camera_take_photo)
    Box(Modifier.fillMaxWidth().padding(bottom = Spacing.section, top = Spacing.content), contentAlignment = Alignment.Center) {
        // Shutter: a primary disc inside a ring.
        Box(
            Modifier
                .size(84.dp)
                .border(4.dp, CameraAccent, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(CameraAccent)
                .alpha(if (enabled) 1f else 0.5f)
                .clickable(enabled = enabled && !busy, onClick = onShoot)
                .semantics {
                    contentDescription = take
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(28.dp), color = OnCameraAccent, strokeWidth = 3.dp)
            } else {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, tint = OnCameraAccent)
            }
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.section)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onGallery)
                .padding(Spacing.grid),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(Size.minTouchTarget).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2C2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null, tint = Color.White)
            }
            Text(stringResource(R.string.camera_gallery), style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun PermissionPanel(
    panel: CameraPanel,
    onAllow: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, text) = when (panel) {
        CameraPanel.PERMISSION -> Triple(R.drawable.ic_photo_camera, R.string.camera_permission_title, R.string.camera_permission_text)
        CameraPanel.DENIED -> Triple(R.drawable.ic_no_photography, R.string.camera_denied_title, R.string.camera_denied_text)
        CameraPanel.NO_CAMERA -> Triple(R.drawable.ic_no_photography, R.string.camera_none_title, R.string.camera_none_text)
        CameraPanel.FAILED, CameraPanel.NONE -> Triple(R.drawable.ic_error, R.string.camera_failed_title, R.string.camera_failed_text)
    }
    Column(modifier.fillMaxWidth().padding(Spacing.section), verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.content, Alignment.CenterVertically),
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text(
                stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(text),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            if (panel == CameraPanel.PERMISSION) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.Top) {
                    Icon(
                        painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.camera_permission_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            val primary = when (panel) {
                CameraPanel.PERMISSION -> R.string.camera_permission_allow to onAllow
                CameraPanel.DENIED -> R.string.camera_open_settings to onSettings
                CameraPanel.NO_CAMERA -> R.string.camera_gallery_action to onGallery
                CameraPanel.FAILED, CameraPanel.NONE -> R.string.camera_try_again to onRetry
            }
            Button(onClick = primary.second, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                Text(stringResource(primary.first))
            }
            if (panel != CameraPanel.NO_CAMERA) {
                OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth().heightIn(min = Size.minTouchTarget)) {
                    Icon(
                        painterResource(R.drawable.ic_photo_library),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = Spacing.small),
                    )
                    Text(stringResource(R.string.camera_gallery), color = Color.White)
                }
            }
        }
    }
}

private val VIEWFINDER = 280.dp

/** The camera is always dark: the prototype's dark-theme primary and its on-color, whatever the app theme. */
private val CameraAccent = Color(0xFF79C8F5)
private val OnCameraAccent = Color(0xFF073B58)
