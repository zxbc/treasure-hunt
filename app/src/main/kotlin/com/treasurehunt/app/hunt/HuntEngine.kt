package com.treasurehunt.app.hunt

import android.content.Context
import android.location.Location
import android.os.VibrationEffect
import android.os.Vibrator
import com.treasurehunt.app.data.LocationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton that owns the live hunt state: the active list snapshot, the
 * current nearest location, distance/ratio and buzz urgency. The
 * [HuntService] feeds location fixes in; the UI observes [state].
 */
object HuntEngine {

    data class LocationSnapshot(
        val id: Long,
        val name: String,
        val description: String,
        val lat: Double,
        val lon: Double,
        val muted: Boolean,
    ) {
        companion object {
            fun from(entity: LocationEntity): LocationSnapshot =
                LocationSnapshot(entity.id, entity.name, entity.description, entity.lat, entity.lon, entity.muted)
        }
    }

    data class Nearest(
        val snapshot: LocationSnapshot,
        val distanceM: Double,
        val ratio: Float, // distance / radius, 0..1 (0 = exactly on top of it)
        val level: Int, // 0..4 urgency; 4 = hottest
    )

    data class State(
        val isActive: Boolean = false,
        val listId: Long = 0,
        val listName: String = "",
        val radiusM: Int = 500,
        val nearest: Nearest? = null,
        val hasFix: Boolean = false,
        val lastFixAtMs: Long = 0L,
        val totalLocations: Int = 0,
        val mutedLocations: List<LocationSnapshot> = emptyList(),
    )

    val urgencyLabels = listOf("COLD", "WARM", "WARMER", "HOT", "BURNING HOT")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private lateinit var appContext: Context
    private lateinit var vibrator: Vibrator
    private var activeLocations: List<LocationSnapshot> = emptyList()
    private var lastUserFix: Location? = null
    private var lastBuzzAtMs = 0L

    fun init(context: Context) {
        appContext = context.applicationContext
        vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun prefs() = appContext.getSharedPreferences("hunt_prefs", Context.MODE_PRIVATE)

    fun radiusFromPrefs(): Int = prefs().getInt("radius", 500)
    fun activeListFromPrefs(): Long = prefs().getLong("active_list", 0L)

    fun saveRadius(meters: Int) {
        prefs().edit().putInt("radius", meters).apply()
        _state.update { it.copy(radiusM = meters) }
    }

    fun saveActiveList(id: Long) {
        prefs().edit().putLong("active_list", id).apply()
    }

    fun startHunt(listId: Long, listName: String, locations: List<LocationSnapshot>) {
        activeLocations = locations
        lastUserFix = null
        lastBuzzAtMs = 0L
        _state.update {
            it.copy(
                isActive = true,
                listId = listId,
                listName = listName,
                radiusM = radiusFromPrefs(),
                nearest = null,
                hasFix = false,
                lastFixAtMs = 0L,
                totalLocations = locations.size,
                mutedLocations = locations.filter { l -> l.muted },
            )
        }
    }

    fun stopHunt() {
        activeLocations = emptyList()
        lastUserFix = null
        try { vibrator.cancel() } catch (_: Exception) {}
        _state.update {
            it.copy(
                isActive = false,
                listId = 0L,
                listName = "",
                nearest = null,
                hasFix = false,
                lastFixAtMs = 0L,
                totalLocations = 0,
                mutedLocations = emptyList(),
            )
        }
    }

    fun onFix(location: Location) {
        if (!_state.value.isActive) return
        lastUserFix = location
        recomputeNearest()
        val nearest = _state.value.nearest
        if (nearest != null && nearest.distanceM <= _state.value.radiusM) {
            val now = System.currentTimeMillis()
            if (now - lastBuzzAtMs > 1200L) {
                lastBuzzAtMs = now
                buzz(nearest.ratio, nearest.level)
            }
        }
    }

    private fun recomputeNearest() {
        val fix = lastUserFix ?: return
        val radius = _state.value.radiusM
        val user = Location("user")
        user.latitude = fix.latitude
        user.longitude = fix.longitude

        var best: LocationSnapshot? = null
        var bestDistance = Double.MAX_VALUE
        for (candidate in activeLocations) {
            if (candidate.muted) continue
            val target = Location("target")
            target.latitude = candidate.lat
            target.longitude = candidate.lon
            val distance = user.distanceTo(target)
            if (distance < bestDistance) {
                bestDistance = distance
                best = candidate
            }
        }

        val nearest = best?.let {
            val ratio = (bestDistance / radius).toFloat().coerceIn(0f, 1f)
            val level = (4 - (ratio * 5f).toInt()).coerceIn(0, 4)
            Nearest(it, bestDistance, ratio, level)
        }

        _state.update {
            it.copy(
                nearest = nearest,
                hasFix = true,
                lastFixAtMs = System.currentTimeMillis(),
                mutedLocations = activeLocations.filter { l -> l.muted },
            )
        }
    }

    /** Mutes the currently nearest (active) location. Returns the muted location id. */
    fun muteNearestLocation(): Long? {
        val nearest = _state.value.nearest ?: return null
        val id = nearest.snapshot.id
        activeLocations = activeLocations.map { if (it.id == id) it.copy(muted = true) else it }
        recomputeNearest()
        return id
    }

    fun unmuteLocation(id: Long) {
        activeLocations = activeLocations.map { if (it.id == id) it.copy(muted = false) else it }
        recomputeNearest()
    }

    fun mutedLocationId(): Long? = _state.value.nearest?.snapshot?.id

    /**
     * Buzzes with a pattern scaled by proximity: the closer to the target,
     * the longer each pulse, the shorter the gap and the more repeats.
     */
    fun buzz(ratio: Float, level: Int) {
        val buzzMs = (50 + 260 * (1 - ratio)).toLong().coerceIn(40L, 320L)
        val gapMs = (1700 - 1500 * (1 - ratio)).toLong().coerceIn(140L, 1700L)
        val repeats = level + 1
        val pattern = ArrayList<Long>()
        pattern.add(-1L)
        for (i in 0 until repeats) {
            pattern.add(buzzMs)
            if (i < repeats - 1) pattern.add(gapMs)
        }
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern.toLongArray(), -1))
        } catch (_: Exception) {}
    }
}
