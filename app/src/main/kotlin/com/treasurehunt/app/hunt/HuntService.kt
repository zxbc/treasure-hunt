package com.treasurehunt.app.hunt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.treasurehunt.app.R
import com.treasurehunt.app.data.HuntDatabase
import com.treasurehunt.app.data.Repository
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HuntService : Service(), LocationListener {

    companion object {
        const val ACTION_STOP = "com.treasurehunt.app.action.STOP"
        const val ACTION_MUTE_NEAREST = "com.treasurehunt.app.action.MUTE_NEAREST"
        const val EXTRA_LIST_ID = "listId"
        const val CHANNEL_ID = "hunt_alerts"
        private const val NOTIFICATION_ID = 42
    }

    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var repo: Repository
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        HuntEngine.init(applicationContext)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationManager = getSystemService(NotificationManager::class.java)
        repo = Repository(HuntDatabase.get(this))
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopHuntAndSelf()
                return START_NOT_STICKY
            }
            ACTION_MUTE_NEAREST -> {
                scope.launch {
                    val id = HuntEngine.muteNearestLocation()
                    if (id != null) repo.setLocationMuted(id, true)
                }
                return START_STICKY
            }
            else -> {
                val listId = intent?.getLongExtra(EXTRA_LIST_ID, 0L) ?: 0L
                loadAndStart(listId)
                return START_STICKY
            }
        }
    }

    private fun loadAndStart(listId: Long) {
        scope.launch {
            startForeground(NOTIFICATION_ID, buildNotification(HuntEngine.state.value))
            val list = repo.get(listId)
            if (list == null || list.locations.isEmpty()) {
                stopHuntAndSelf()
                return@launch
            }
            HuntEngine.startHunt(
                listId = listId,
                listName = list.name,
                locations = list.locations.map { HuntEngine.LocationSnapshot.from(it) },
            )
            acquireWakeLock()
            registerLocationUpdates()

            scope.launch {
                HuntEngine.state.collect { s ->
                    if (s.isActive) {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(s))
                    }
                }
            }
        }
    }

    private fun registerLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val providers = mutableListOf<String>()
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    3000L,
                    5f,
                    this,
                    android.os.Looper.getMainLooper(),
                )
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        HuntEngine.onFix(location)
    }

    private fun stopHuntAndSelf() {
        runCatching {
            locationManager.removeUpdates(this)
        }
        releaseWakeLock()
        HuntEngine.stopHunt()
        stopForeground(true)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "treasurehunt:hunt-active").apply {
                acquire(8 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        wakeLock = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hunt alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Shows the active treasure hunt and its current nearest target"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun serviceIntent(action: String): Intent =
        Intent(this, HuntService::class.java).setAction(action)

    private fun buildNotification(state: HuntEngine.State): Notification {
        val mutePending = PendingIntent.getService(
            this, 1, serviceIntent(ACTION_MUTE_NEAREST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPending = PendingIntent.getService(
            this, 2, serviceIntent(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nearest = state.nearest
        var title = ""
        var text = ""
        if (!state.hasFix) {
            title = "Treasure hunt active"
            text = "Waiting for a location fix…"
        } else if (nearest == null) {
            title = "Treasure hunt active"
            text = "No active targets (all muted)"
        } else {
            title = nearest.snapshot.name
            val distanceText = formatDistance(nearest.distanceM)
            if (nearest.distanceM <= state.radiusM) {
                title = HuntEngine.urgencyLabels[nearest.level] + " — " + nearest.snapshot.name
                text = distanceText + " away — buzzing harder as you get closer"
            } else {
                text = distanceText + " away — outside the " + formatDistance(state.radiusM.toDouble()) + " alert radius"
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hunt)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(NotificationCompat.Action.Builder(null, "Mute nearest", mutePending).build())
            .addAction(NotificationCompat.Action.Builder(null, "Stop hunt", stopPending).build())
            .build()
    }

    private fun formatDistance(meters: Double): String =
        if (meters < 1000) meters.roundToInt().toString() + " m"
        else String.format("%.1f km", meters / 1000.0)

    override fun onDestroy() {
        runCatching { locationManager.removeUpdates(this) }
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
