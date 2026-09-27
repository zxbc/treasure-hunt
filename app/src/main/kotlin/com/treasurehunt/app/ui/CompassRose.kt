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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treasurehunt.app.R

/**
 * A compass rose that points at the nearest active spot of the current hunt.
 *
 * The needle angle is the geographic bearing from the user to the target
 * (computed by [com.treasurehunt.app.hunt.HuntEngine]) minus the phone's
 * current azimuth (from the rotation vector sensor), so the needle keeps
 * pointing at the target while the phone is turned. The needle follows with a
 * short spring and takes the urgency color of the current proximity level,
 * matching the gauge above it.
 */
@Composable
fun CompassRose(
    targetBearing: Float,
    needleColor: Color,
    modifier: Modifier = Modifier.size(200.dp),
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

    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f

            // Outer ring
            drawCircle(
                color = outline,
                radius = r - 1.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // Tick marks: fine every 15 degrees, longer at the intercardinals,
            // longest at the cardinals.
            for (deg in 0 until 360 step 15) {
                val tickLength = when (deg % 90) {
                    0 -> 10.dp.toPx()
                    45 -> 8.dp.toPx()
                    else -> 4.5.dp.toPx()
                }
                val tickColor = if (deg % 90 == 0) onSurfaceVariant else outline
                val tickWidth = if (deg % 90 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                rotate(degrees = deg.toFloat(), pivot = Offset(cx, cy)) {
                    val edge = r - 2.5.dp.toPx()
                    drawLine(
                        color = tickColor,
                        start = Offset(cx, cy - edge),
                        end = Offset(cx, cy - edge + tickLength),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Needle: the target half is colored by urgency, the tail is muted.
            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                val tip = r * 0.52f
                val halfWidth = r * 0.16f
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

            // Center cap
            drawCircle(onBackground, radius = r * 0.08f)
            drawCircle(
                color = outline,
                radius = r * 0.08f,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        // Cardinal labels, offset inward from the ring.
        val labelRadius = 64.dp
        Text(
            stringResource(R.string.cardinal_n),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center).offset(y = -labelRadius),
        )
        Text(
            stringResource(R.string.cardinal_e),
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).offset(x = labelRadius),
        )
        Text(
            stringResource(R.string.cardinal_s),
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).offset(y = labelRadius),
        )
        Text(
            stringResource(R.string.cardinal_w),
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).offset(x = -labelRadius),
        )
    }
}

/** Compass caption below the rose: compass direction plus a hint. */
@Composable
fun CompassCaption(targetBearing: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        stringResource(
            R.string.compass_caption,
            cardinalDirection(context, targetBearing),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private val CardinalRes = intArrayOf(
    R.string.cardinal_n,
    R.string.cardinal_ne,
    R.string.cardinal_e,
    R.string.cardinal_se,
    R.string.cardinal_s,
    R.string.cardinal_sw,
    R.string.cardinal_w,
    R.string.cardinal_nw,
)

private fun cardinalDirection(context: Context, bearing: Float): String {
    val index = ((bearing + 22.5f) % 360f).toInt() / 45
    return context.getString(CardinalRes[index.coerceIn(0, CardinalRes.size - 1)])
}

/**
 * Reads the device azimuth (degrees clockwise from magnetic north) from the
 * rotation vector sensor. [Float.NaN] until the first reading. Sensor events
 * are delivered on the main thread, so reading this snapshot state in
 * composition recomposes when it changes.
 */
@Composable
private fun rememberDeviceAzimuth(): FloatState {
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
