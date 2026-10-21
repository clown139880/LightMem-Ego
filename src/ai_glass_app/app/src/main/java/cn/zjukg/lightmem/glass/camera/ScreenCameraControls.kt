package cn.zjukg.lightmem.glass.camera

import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cn.zjukg.lightmem.glass.lightmem_ego.LightMemEgoDiagnostics
import java.util.Locale

@Composable
fun rememberScreenExposureStatus(camera: Camera?, requestedEv: Double): String {
    val context = LocalContext.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    var status by remember { mutableStateOf(exposureLabel(requestedEv)) }

    DisposableEffect(camera) {
        if (camera == null) return@DisposableEffect onDispose { }
        // Keep AE metering on the screen in the middle of the view. Do not lock AF.
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f, 0.25f)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AE)
            .disableAutoCancel()
            .build()
        var active = true
        runCatching {
            if (camera.cameraInfo.isFocusMeteringSupported(action)) {
                val future = camera.cameraControl.startFocusAndMetering(action)
                future.addListener({
                    if (active) {
                        runCatching { future.get() }.onFailure {
                            LightMemEgoDiagnostics.logError(context, "camera-metering-failed", "center AE", it)
                        }
                    }
                }, executor)
            } else {
                LightMemEgoDiagnostics.log(context, "camera-metering-unsupported", "center AE")
            }
        }.onFailure {
            LightMemEgoDiagnostics.logError(context, "camera-metering-failed", "center AE", it)
        }
        onDispose { active = false }
    }

    DisposableEffect(camera, requestedEv) {
        status = exposureLabel(requestedEv)
        if (camera == null) return@DisposableEffect onDispose { }
        val exposure = camera.cameraInfo.exposureState
        val range = exposure.exposureCompensationRange
        val step = exposure.exposureCompensationStep.toDouble()
        val index = exposureIndexForEv(requestedEv, step, range.lower, range.upper)
        if (!exposure.isExposureCompensationSupported || index == null) {
            status = "EV unsupported"
            LightMemEgoDiagnostics.log(context, "camera-exposure-unsupported", "range=$range step=$step")
            return@DisposableEffect onDispose { }
        }
        var active = true
        status = "${exposureLabel(requestedEv)}..."
        runCatching {
            val future = camera.cameraControl.setExposureCompensationIndex(index)
            future.addListener({
                if (active) {
                    runCatching { future.get() }.onSuccess { appliedIndex ->
                        status = exposureLabel(appliedIndex * step)
                        LightMemEgoDiagnostics.log(
                            context, "camera-exposure-applied",
                            "requestedEv=$requestedEv appliedEv=${appliedIndex * step} index=$appliedIndex range=$range step=$step",
                        )
                    }.onFailure {
                        status = "EV apply failed"
                        LightMemEgoDiagnostics.logError(context, "camera-exposure-failed", "requestedEv=$requestedEv", it)
                    }
                }
            }, executor)
        }.onFailure {
            status = "EV apply failed"
            LightMemEgoDiagnostics.logError(context, "camera-exposure-failed", "requestedEv=$requestedEv", it)
        }
        onDispose { active = false }
    }
    return status
}

private fun exposureLabel(ev: Double): String = String.format(Locale.US, "EV %+.1f", ev)
