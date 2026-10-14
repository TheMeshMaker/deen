/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.prayer.QiblaFinder
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberHaptics
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class QiblaUiState(
    val bearing: Double? = null,
    val distanceKm: Double? = null,
    val coordinates: Coordinates? = null,
    val place: io.github.themeshmaker.dunya.core.model.SavedLocation? = null
)

@HiltViewModel
class QiblaViewModel @Inject constructor(userData: UserDataRepository, qibla: QiblaFinder) : ViewModel() {
    val uiState: StateFlow<QiblaUiState> = userData.userData.map { data ->
        val location = data.location ?: return@map QiblaUiState()
        QiblaUiState(qibla.bearing(location.coordinates), qibla.distanceKm(location.coordinates), location.coordinates, location)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState())
}

/** Phone heading in degrees from true north, and whether the compass needs calibrating. */
private data class Heading(val degrees: Float, val unreliable: Boolean)

@Composable
internal fun QiblaScreenRoute(onBack: () -> Unit, viewModel: QiblaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val heading by rememberHeading(state.coordinates)
    val haptics = rememberHaptics(enabled = true)
    val bearing = state.bearing
    val offset = if (bearing != null && heading != null) angleDiff(bearing.toFloat(), heading!!.degrees) else null
    val aligned = offset != null && abs(offset) <= ALIGNED_DEGREES
    LaunchedEffect(aligned) { if (aligned) haptics.done() }
    // Facing the Qibla: a soft halo breathes around the dial (Sleek).
    val motion = DeenTheme.look.motion
    val glow = if (aligned && motion) {
        rememberInfiniteTransition(label = "halo").animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_100, easing = DeenMotion.EaseInOutSoft), RepeatMode.Reverse),
            label = "glow"
        ).value
    } else {
        0f
    }

    DeenScaffold(
        title = stringResource(R.string.feature_qibla_title),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back),
        largeTitle = false
    ) { padding ->
        val look = DeenTheme.look
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            if (bearing == null) {
                Text(stringResource(R.string.feature_qibla_no_location), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            Text(
                when {
                    heading == null -> stringResource(R.string.feature_qibla_no_compass)
                    aligned -> stringResource(R.string.feature_qibla_aligned)
                    offset!! > 0 -> stringResource(R.string.feature_qibla_turn_right, localDigits(abs(offset).roundToInt()))
                    else -> stringResource(R.string.feature_qibla_turn_left, localDigits(abs(offset!!).roundToInt()))
                },
                style = MaterialTheme.typography.titleLarge,
                color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (aligned) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            Compass(
                heading = heading?.degrees ?: 0f,
                qibla = bearing.toFloat(),
                aligned = aligned,
                glow = glow,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            DeenCard {
                Text(
                    stringResource(R.string.feature_qibla_bearing, localDigits(bearing.roundToInt())),
                    style = MaterialTheme.typography.titleMedium
                )
                state.distanceKm?.let {
                    Text(
                        stringResource(R.string.feature_qibla_distance, localDigits(it.roundToInt())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.place?.let {
                    Text(it.displayName(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                stringResource(if (heading?.unreliable == true) R.string.feature_qibla_calibrate else R.string.feature_qibla_tip),
                style = MaterialTheme.typography.bodySmall,
                color = if (heading?.unreliable == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A dial that turns with the phone: north marker, ticks, and the Kaaba at the Qibla bearing. */
@Composable
private fun Compass(heading: Float, qibla: Float, aligned: Boolean, glow: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val gradient = DeenTheme.gradient
    val rim by animateColorAsState(if (aligned) colors.primary else colors.outlineVariant, tween(300), label = "rim")
    Canvas(modifier) {
        val radius = size.minDimension / 2f - 10.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        if (glow > 0f) {
            drawCircle(colors.primary.copy(alpha = 0.18f * glow), radius + 10.dp.toPx() * glow, center)
        }
        drawCircle(colors.surfaceContainerHigh, radius)
        drawCircle(rim, radius, style = Stroke(3.dp.toPx()))
        rotate(-heading, center) {
            for (deg in 0 until 360 step 15) {
                rotate(deg.toFloat(), center) {
                    val long = deg % 90 == 0
                    drawLine(
                        color = if (deg == 0) colors.error else colors.onSurfaceVariant.copy(alpha = if (long) 0.9f else 0.4f),
                        start = Offset(center.x, center.y - radius + 8.dp.toPx()),
                        end = Offset(center.x, center.y - radius + (if (long) 26 else 16).dp.toPx()),
                        strokeWidth = (if (long) 3f else 1.5f).dp.toPx()
                    )
                }
            }
            // The Kaaba: a dark cube with a gold band, on the Qibla bearing.
            rotate(qibla, center) {
                val side = radius * 0.22f
                val top = center.y - radius * 0.72f
                drawLine(gradient.start, center, Offset(center.x, top + side), strokeWidth = 4.dp.toPx())
                drawRoundRect(Color(0xFF151515), Offset(center.x - side / 2, top), Size(side, side), CornerRadius(4.dp.toPx()))
                drawRect(Color(0xFFD4AF37), Offset(center.x - side / 2, top + side * 0.22f), Size(side, side * 0.14f))
            }
        }
        // Fixed pointer: where the top of the phone faces.
        val tip = Offset(center.x, center.y - radius - 2.dp.toPx())
        val pointer = Path().apply {
            moveTo(tip.x, tip.y + 22.dp.toPx())
            lineTo(tip.x - 12.dp.toPx(), tip.y)
            lineTo(tip.x + 12.dp.toPx(), tip.y)
            close()
        }
        drawPath(pointer, if (aligned) colors.primary else colors.onSurface)
        drawCircle(if (aligned) colors.primary else colors.onSurfaceVariant, 6.dp.toPx(), center)
    }
}

/** Signed shortest turn from [current] to [target], −180..180 (positive = turn right). */
private fun angleDiff(target: Float, current: Float): Float = ((target - current + 540f) % 360f) - 180f

private const val ALIGNED_DEGREES = 3f

/**
 * Listens to the rotation-vector sensor (accelerometer + magnetometer as a fallback) while on
 * screen, corrects for the display rotation and for magnetic declination at [at], and smooths
 * the result. Null when the phone has no compass.
 */
@Composable
private fun rememberHeading(at: Coordinates?): State<Heading?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Heading?>(null) }
    DisposableEffect(context, at) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val rotationVector = sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensors?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val declination =
            at?.let { GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis()).declination } ?: 0f
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var haveGravity = false
        var haveMagnetic = false
        var smoothed: Float? = null
        var unreliable = false

        fun publish() {
            val (axisX, axisY) = when (context.displayRotation()) {
                Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            }
            SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped)
            SensorManager.getOrientation(remapped, orientation)
            val raw = (Math.toDegrees(orientation[0].toDouble()).toFloat() + declination + 360f) % 360f
            val previous = smoothed
            smoothed = if (previous == null) raw else (previous + angleDiff(raw, previous) * SMOOTHING + 360f) % 360f
            heading.value = Heading(smoothed!!, unreliable)
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotation, event.values)
                        publish()
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        event.values.copyInto(gravity, endIndex = 3)
                        haveGravity = true
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        event.values.copyInto(geomagnetic, endIndex = 3)
                        haveMagnetic = true
                    }
                }
                if (rotationVector == null && haveGravity && haveMagnetic &&
                    SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
                ) {
                    publish()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    unreliable = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }
        if (rotationVector != null) {
            sensors.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            magnetometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        } else if (accelerometer != null && magnetometer != null) {
            sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensors.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensors?.unregisterListener(listener) }
    }
    return heading
}

private const val SMOOTHING = 0.15f

@Suppress("DEPRECATION")
private fun Context.displayRotation(): Int = runCatching {
    if (android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.R
    ) {
        display.rotation
    } else {
        getSystemService(WindowManager::class.java).defaultDisplay.rotation
    }
}.getOrDefault(Surface.ROTATION_0)
