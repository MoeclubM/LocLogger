package moe.telecom.loclogger.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import moe.telecom.loclogger.MainActivity
import moe.telecom.loclogger.R
import moe.telecom.loclogger.data.local.AnnotationDao
import moe.telecom.loclogger.data.local.TrackDao
import moe.telecom.loclogger.data.local.TrackPointDao
import moe.telecom.loclogger.data.local.entity.AnnotationEntity
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.local.entity.TrackPointEntity
import moe.telecom.loclogger.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class TrackingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentTrackId: Long? = null,
    val trackName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val bearing: Float? = null,
    val satellitesUsed: Int = 0,
    val satellitesVisible: Int = 0,
    val pointCount: Int = 0,
    val annotationCount: Int = 0,
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxAltitude: Double? = null,
    val minAltitude: Double? = null,
    val altitudeDiff: Double = 0.0,
    val startTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var trackDao: TrackDao
    @Inject lateinit var trackPointDao: TrackPointDao
    @Inject lateinit var annotationDao: AnnotationDao
    @Inject lateinit var settingsRepository: SettingsRepository

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager

    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private var currentTrackId: Long? = null
    private var lastLocation: Location? = null
    private var startTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var lastPauseTime: Long = 0L
    private var totalDistance: Double = 0.0
    private var maxSpeed: Float = 0f
    private var pointCount: Int = 0
    private var maxAlt: Double? = null
    private var minAlt: Double? = null
    private var durationUpdateJob: Job? = null
    private var locationUpdatesStarted = false

    private val trackNameFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())

    private val locationListener = LocationListener { location ->
        handleLocationUpdate(location)
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            val visible = status.satelliteCount
            for (i in 0 until visible) {
                if (status.usedInFix(i)) used++
            }
            _trackingState.value = _trackingState.value.copy(
                satellitesUsed = used,
                satellitesVisible = visible
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        // 实时定位：服务绑定即开始监听，未录制也能显示定位
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
            ACTION_ADD_ANNOTATION -> {
                val desc = intent.getStringExtra(EXTRA_ANNOTATION_DESC) ?: "批注"
                addAnnotation(desc)
            }
        }
        return START_NOT_STICKY
    }

    fun startTracking() {
        if (_trackingState.value.isRecording) return

        serviceScope.launch {
            startTime = System.currentTimeMillis()
            val trackName = trackNameFormat.format(Date(startTime))
            val track = TrackEntity(
                name = trackName,
                startTime = startTime,
                isRecording = true
            )
            currentTrackId = trackDao.insertTrack(track)

            _trackingState.value = TrackingState(
                isRecording = true,
                currentTrackId = currentTrackId,
                trackName = trackName,
                startTime = startTime
            )

            acquireWakeLock()
            val settings = settingsRepository.settings.first()
            startLocationUpdates(intervalMs = settings.gpsInterval.toLong())
            startForeground(NOTIFICATION_ID, buildNotification("正在记录轨迹…"))
            startDurationUpdates()
        }
    }

    fun pauseTracking() {
        if (!_trackingState.value.isRecording || _trackingState.value.isPaused) return
        lastPauseTime = System.currentTimeMillis()
        _trackingState.value = _trackingState.value.copy(isPaused = true)
        updateNotification("记录已暂停")
    }

    fun resumeTracking() {
        if (!_trackingState.value.isRecording || !_trackingState.value.isPaused) return
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        _trackingState.value = _trackingState.value.copy(isPaused = false)
        updateNotification("正在记录轨迹…")
    }

    fun stopTracking() {
        if (!_trackingState.value.isRecording) return

        serviceScope.launch {
            val endTime = System.currentTimeMillis()
            val trackId = currentTrackId ?: return@launch

            val avgSpeed = if (pointCount > 0 && totalDistance > 0) {
                (totalDistance / ((endTime - startTime - pausedDuration) / 1000.0)).toFloat()
            } else 0f

            val altDiff = if (maxAlt != null && minAlt != null) maxAlt!! - minAlt!! else 0.0

            trackDao.finishTrack(
                id = trackId,
                endTime = endTime,
                pointCount = pointCount,
                distance = totalDistance,
                maxSpeed = maxSpeed,
                avgSpeed = avgSpeed,
                maxAlt = maxAlt,
                minAlt = minAlt,
                altDiff = altDiff
            )

            releaseWakeLock()
            durationUpdateJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            // 清除 started 状态：解绑后服务即销毁；绑定期间仍持续提供实时定位
            stopSelf()

            // 保留实时定位字段，仅清空录制状态；服务继续提供实时定位直到解绑
            val live = _trackingState.value
            _trackingState.value = TrackingState(
                latitude = live.latitude,
                longitude = live.longitude,
                altitude = live.altitude,
                speed = live.speed,
                accuracy = live.accuracy,
                bearing = live.bearing,
                satellitesUsed = live.satellitesUsed,
                satellitesVisible = live.satellitesVisible
            )
            currentTrackId = null
            lastLocation = null
            totalDistance = 0.0
            maxSpeed = 0f
            pointCount = 0
            maxAlt = null
            minAlt = null
            pausedDuration = 0L
        }
    }

    fun addAnnotation(description: String) {
        val state = _trackingState.value
        if (!state.isRecording || state.latitude == null || state.longitude == null) return

        serviceScope.launch {
            val trackId = currentTrackId ?: return@launch
            annotationDao.insertAnnotation(
                AnnotationEntity(
                    trackId = trackId,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    description = description,
                    timestamp = System.currentTimeMillis()
                )
            )
            val count = annotationDao.getAnnotationCount(trackId)
            _trackingState.value = _trackingState.value.copy(annotationCount = count)
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val now = System.currentTimeMillis()
        val state = _trackingState.value

        // 实时定位：无论是否录制都更新坐标，未录制也能显示定位
        _trackingState.value = state.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            speed = if (location.hasSpeed()) location.speed else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            lastUpdateTime = now
        )

        // 仅录制阶段写库与统计
        val trackId = currentTrackId
        if (trackId == null || !_trackingState.value.isRecording || _trackingState.value.isPaused) {
            lastLocation = location
            return
        }
        // 录制轨迹仅采 GPS 高精度点，避免网络兜底把轨迹拉偏
        if (location.provider != LocationManager.GPS_PROVIDER) return

        // 计算距离
        var segmentDistance = 0f
        lastLocation?.let { last ->
            segmentDistance = last.distanceTo(location)
            // 距离过滤：小于3米的点忽略
            if (segmentDistance < 3f) return
            totalDistance += segmentDistance
        }

        // 更新高度极值
        if (location.hasAltitude()) {
            val alt = location.altitude
            if (maxAlt == null || alt > maxAlt!!) maxAlt = alt
            if (minAlt == null || alt < minAlt!!) minAlt = alt
        }

        // 更新最大速度
        if (location.hasSpeed() && location.speed > maxSpeed) {
            maxSpeed = location.speed
        }

        pointCount++

        serviceScope.launch {
            // 插入轨迹点
            trackPointDao.insertPoint(
                TrackPointEntity(
                    trackId = trackId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    speed = if (location.hasSpeed()) location.speed else null,
                    accuracy = if (location.hasAccuracy()) location.accuracy else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    timestamp = now,
                    satellitesUsed = _trackingState.value.satellitesUsed,
                    satellitesVisible = _trackingState.value.satellitesVisible
                )
            )

            // 更新状态
            val elapsed = now - startTime - pausedDuration
            val avgSpd = if (elapsed > 0 && totalDistance > 0) {
                (totalDistance / (elapsed / 1000.0)).toFloat()
            } else 0f

            _trackingState.value = _trackingState.value.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed else null,
                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                pointCount = pointCount,
                distance = totalDistance,
                maxSpeed = maxSpeed,
                avgSpeed = avgSpd,
                maxAltitude = maxAlt,
                minAltitude = minAlt,
                altitudeDiff = if (maxAlt != null && minAlt != null) maxAlt!! - minAlt!! else 0.0,
                lastUpdateTime = now
            )

            // 更新通知
            updateNotification(buildString {
                append("距离: ")
                append(if (totalDistance >= 1000) String.format("%.2f公里", totalDistance / 1000)
                else String.format("%.0f米", totalDistance))
                append(" · 点数: ")
                append(pointCount)
            })
        }

        lastLocation = location
    }

    private fun startLocationUpdates(intervalMs: Long = MIN_TIME_MS) {
        try {
            // 网络定位兜底：GPS 在室内/无卫星信号时也能快速显示实时位置
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMs,
                    MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
            // 优先使用 GPS_PROVIDER 高精度定位，更新周期跟随设置
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMs,
                MIN_DISTANCE_M,
                locationListener,
                Looper.getMainLooper()
            )
            // 注册 GnssStatus 回调获取卫星数（仅首次，避免重复注册）
            if (!locationUpdatesStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // handler 必须绑定 Looper：startLocationUpdates 可能在后台协程线程调用，
                // 传 null 会尝试用当前线程创建 Handler 导致 RuntimeException
                locationManager.registerGnssStatusCallback(gnssCallback, Handler(Looper.getMainLooper()))
                locationUpdatesStarted = true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No location permission", e)
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        if (locationUpdatesStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
            locationUpdatesStarted = false
        }
    }

    private fun startDurationUpdates() {
        durationUpdateJob = serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val state = _trackingState.value
                if (state.isRecording && !state.isPaused) {
                    val elapsed = System.currentTimeMillis() - startTime - pausedDuration
                    _trackingState.value = state.copy(duration = elapsed)
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsLogger:Tracking").apply {
            acquire(30 * 60 * 1000L) // 30分钟超时
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        releaseWakeLock()
        durationUpdateJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "TrackingService"
        private const val CHANNEL_ID = "gps_tracking"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "moe.telecom.loclogger.START_TRACKING"
        const val ACTION_PAUSE = "moe.telecom.loclogger.PAUSE_TRACKING"
        const val ACTION_RESUME = "moe.telecom.loclogger.RESUME_TRACKING"
        const val ACTION_STOP = "moe.telecom.loclogger.STOP_TRACKING"
        const val ACTION_ADD_ANNOTATION = "moe.telecom.loclogger.ADD_ANNOTATION"
        const val EXTRA_ANNOTATION_DESC = "annotation_desc"

        private const val MIN_TIME_MS = 1000L
        private const val MIN_DISTANCE_M = 0f

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun addAnnotation(context: Context, description: String) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_ADD_ANNOTATION
                putExtra(EXTRA_ANNOTATION_DESC, description)
            }
            context.startService(intent)
        }
    }
}
