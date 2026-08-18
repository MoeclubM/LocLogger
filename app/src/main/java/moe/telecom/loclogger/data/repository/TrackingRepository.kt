package moe.telecom.loclogger.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import moe.telecom.loclogger.data.export.TrackExporter
import moe.telecom.loclogger.data.local.AnnotationDao
import moe.telecom.loclogger.data.local.TrackDao
import moe.telecom.loclogger.data.local.TrackPointDao
import moe.telecom.loclogger.data.local.entity.AnnotationEntity
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.local.entity.TrackPointEntity
import moe.telecom.loclogger.data.service.TrackingService
import moe.telecom.loclogger.data.service.TrackingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val annotationDao: AnnotationDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateCollectionJob: Job? = null

    private var trackingService: TrackingService? = null
    private var bound = false

    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TrackingService.LocalBinder
            trackingService = binder.getService()
            bound = true
            // 收集服务状态
            stateCollectionJob?.cancel()
            stateCollectionJob = scope.launch {
                trackingService?.trackingState?.collect { state ->
                    _trackingState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            bound = false
            stateCollectionJob?.cancel()
            stateCollectionJob = null
        }
    }

    fun bindService() {
        if (!bound) {
            val intent = Intent(context, TrackingService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startTracking() {
        TrackingService.start(context)
        bindService()
    }

    fun pauseTracking() {
        TrackingService.pause(context)
    }

    fun resumeTracking() {
        TrackingService.resume(context)
    }

    fun stopTracking(name: String? = null, activityType: Int? = null) {
        TrackingService.stop(context, name, activityType)
    }

    fun addAnnotation(description: String) {
        TrackingService.addAnnotation(context, description)
    }

    // 数据库操作
    fun getAllTracks(): Flow<List<TrackEntity>> = trackDao.getAllTracks()
    suspend fun getPointsForTrackSync(trackId: Long): List<TrackPointEntity> =
        trackPointDao.getPointsForTrackSync(trackId)
    suspend fun getAnnotationsForTrackSync(trackId: Long): List<AnnotationEntity> =
        annotationDao.getAnnotationsForTrackSync(trackId)
    suspend fun getTrackById(id: Long): TrackEntity? = trackDao.getTrackById(id)
    suspend fun deleteTrack(track: TrackEntity) = trackDao.deleteTrack(track)
    suspend fun renameTrack(id: Long, name: String) = trackDao.renameTrack(id, name)
    suspend fun updateActivityType(id: Long, activityType: Int) = trackDao.updateActivityType(id, activityType)

    // 导出
    fun exportTrack(
        out: java.io.OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>,
        annotations: List<AnnotationEntity>,
        format: String,
        gpxVersion: String = "1.1"
    ) {
        when (format) {
            "gpx" -> TrackExporter.exportGpx(out, track, points, annotations, gpxVersion)
            "kml" -> TrackExporter.exportKml(out, track, points, annotations)
            "kmz" -> TrackExporter.exportKmz(out, track, points, annotations)
            "csv" -> TrackExporter.exportCsv(out, track, points)
            "json" -> TrackExporter.exportJson(out, track, points, annotations)
            else -> TrackExporter.exportTxt(out, track, points, annotations)
        }
    }

    fun getExportFileName(track: TrackEntity, format: String): String =
        TrackExporter.getExportFileName(track, format)

    fun getExportMimeType(format: String): String =
        TrackExporter.getExportMimeType(format)
}
