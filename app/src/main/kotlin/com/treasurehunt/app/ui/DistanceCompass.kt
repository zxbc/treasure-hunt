package com.treasurehunt.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treasurehunt.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * The main hunt dial: the compass rose and the urgency gauge merged into one
 * circle. Compass ticks and cardinal labels sit on the outer perimeter, the
 * urgency ring (and needle) point at the tracked spot, and the distance
 * display sits in the middle.
 *
 * The needle angle is the geographic bearing from the user to the target
 * (computed by [com.treasurehunt.app.hunt.HuntEngine]) minus the phone's
 * current azimuth (from the rotation vector sensor), so the needle keeps
 * pointing at the target while the phone is turned. The needle follows with a
 * short spring and takes the urgency color of the current proximity level.
 */
@Composable
fun DistanceCompass(
    targetBearing: Float,
    progress: Float,
    color: Color,
    needleColor: Color,
    centerText: String,
    subText: String,
    modifier: Modifier = Modifier.size(320.dp),
) {
    val azimuth = rememberDeviceAzimuth()

    // Unwrap the needle's target angle against its previous value so the
    // animation always travels the short way around the dial.
    val targetAngle = remember { mutableFloatStateOf(0f) }
    if (!azimuth.floatValue.isNaN()) {
        var unwrapped = targetBearing - azimuth.floatValue
        val current = targetAngle.floatValue
        while (unwrapped - current > 180f) unwrapped -= 360f
        while (unwrapped - current < -180f) unwrapped += 360f
        if (unwrapped != current) targetAngle.floatValue = unwrapped
    }
    val angle by animateFloatAsState(
        targetValue = targetAngle.floatValue,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "compass-needle",
    )

    // Ring rotation: the ticks and cardinal labels turn with the phone (like
    // a real compass), so north always points at true north. Unwrapped and
    // springed with the same spec as the needle, which keeps the needle fixed
    // against the ring at the target's true bearing.
    val ringTarget = remember { mutableFloatStateOf(0f) }
    if (!azimuth.floatValue.isNaN()) {
        var unwrapped = -azimuth.floatValue
        val current = ringTarget.floatValue
        while (unwrapped - current > 180f) unwrapped -= 360f
        while (unwrapped - current < -180f) unwrapped += 360f
        if (unwrapped != current) ringTarget.floatValue = unwrapped
    }
    val ringRotation by animateFloatAsState(
        targetValue = ringTarget.floatValue,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "compass-ring",
    )

    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onBackground = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surface

    // Radii of the inner elements, for the default 320.dp dial size
    // (half of it is 160.dp).
    val discRadius = 70.dp
    val labelRadius = 131.dp

    Box(modifier.clip(CircleShape).background(surface)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f

            // Outer compass ring
            drawCircle(
                color = outline,
                radius = r - 1.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // Compass ticks: fine every 15 degrees, longer at the
            // intercardinals, longest at the cardinals.
            for (deg in 0 until 360 step 15) {
                val tickLength = when (deg % 90) {
                    0 -> 11.dp.toPx()
                    45 -> 8.dp.toPx()
                    else -> 4.5.dp.toPx()
                }
                val tickColor = if (deg % 90 == 0) onSurfaceVariant else outline
                val tickWidth = if (deg % 90 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                rotate(degrees = deg.toFloat() + ringRotation, pivot = Offset(cx, cy)) {
                    val edge = r - 3.dp.toPx()
                    drawLine(
                        color = tickColor,
                        start = Offset(cx, cy - edge),
                        end = Offset(cx, cy - edge + tickLength),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Urgency ring: a 270-degree meter between the compass and the
            // center, filled by [progress] and colored [color].
            val arcRadius = r * 0.68f
            val arcRect = androidx.compose.ui.geometry.Rect(
                cx - arcRadius,
                cy - arcRadius,
                cx + arcRadius,
                cy + arcRadius,
            )
            val arcPath = Path().apply { addArc(arcRect, 135f, 270f) }
            drawPath(
                arcPath,
                outline,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
            )
            if (progress > 0.002f) {
                val progressPath = Path().apply {
                    addArc(arcRect, 135f, 270f * progress)
                }
                drawPath(
                    progressPath,
                    color,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Needle: the target half is colored by urgency, the tail is
            // muted. It is drawn before the center disc, which covers its
            // pivot so the needle appears to emerge from the distance
            // display and point at the tracked spot.
            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                val tip = r * 0.58f
                val halfWidth = r * 0.13f
                val targetHalf = Path().apply {
                    moveTo(cx, cy - tip)
                    lineTo(cx + halfWidth, cy)
                    lineTo(cx - halfWidth, cy)
                    close()
                }
                drawPath(targetHalf, needleColor, style = Fill)
                val tailHalf = Path().apply {
                    moveTo(cx, cy + tip * 0.55f)
                    lineTo(cx + halfWidth * 0.72f, cy)
                    lineTo(cx - halfWidth * 0.72f, cy)
                    close()
                }
                drawPath(tailHalf, outline, style = Fill)
            }

            // Center disc behind the distance display
            val discRadiusPx = discRadius.toPx()
            drawCircle(color = surface, radius = discRadiusPx)
            drawCircle(
                color = outline,
                radius = discRadiusPx,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Cardinal labels, rotated with the ring so they always sit at their
        // true bearings.
        CardinalLabel(
            text = stringResource(R.string.cardinal_n),
            bearing = 0f,
            ringRotation = ringRotation,
            radius = labelRadius,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        CardinalLabel(
            text = stringResource(R.string.cardinal_e),
            bearing = 90f,
            ringRotation = ringRotation,
            radius = labelRadius,
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
        )
        CardinalLabel(
            text = stringResource(R.string.cardinal_s),
            bearing = 180f,
            ringRotation = ringRotation,
            radius = labelRadius,
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
        )
        CardinalLabel(
            text = stringResource(R.string.cardinal_w),
            bearing = 270f,
            ringRotation = ringRotation,
            radius = labelRadius,
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
        )

        // Distance display in the middle.
        Box(
            modifier = Modifier
                .size(discRadius * 2)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            Column() {
                Text(
                    centerText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackground,
                )
                Text(
                    subText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

/**
 * One cardinal label of the rotating ring. [bearing] is the label's true
 * bearing (0 = north, clockwise); [ringRotation] turns it with the phone so
 * the label always sits at its true bearing on the dial.
 */
@Composable
private fun BoxScope.CardinalLabel(
    text: String,
    bearing: Float,
    ringRotation: Float,
    radius: Dp,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
) {
    val radians = Math.toRadians((bearing + ringRotation).toDouble())
    Text(
        text,
        style = style,
        color = color,
        fontWeight = fontWeight ?: FontWeight.Normal,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(
                x = (radius.value * sin(radians)).dp,
                y = (-radius.value * cos(radians)).dp,
            ),
    )
}

/**
 * Reads the device azimuth (degrees clockwise from magnetic north) from the
 * rotation vector sensor. [Float.NaN] until the first reading. Sensor events
 * are delivered on the main thread, so reading this snapshot state in
 * composition recomposes when it changes.
 */
@Composable
fun rememberDeviceAzimuth(): FloatState {
    val context = LocalContext.current
    val azimuth = remember { mutableFloatStateOf(Float.NaN) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                @Suppress("DEPRECATION")
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuth.floatValue = ((Math.toDegrees(orientation[0].toDouble()) % 360.0) + 360.0)
                    .toFloat() % 360.0f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            if (sensor != null) sensorManager.unregisterListener(listener)
        }
    }
    return azimuth
}
