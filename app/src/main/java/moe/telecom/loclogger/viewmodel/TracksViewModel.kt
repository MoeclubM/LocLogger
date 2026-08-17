package moe.telecom.loclogger.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.repository.SettingsRepository
import moe.telecom.loclogger.data.repository.TrackingRepository
import moe.telecom.loclogger.ui.screen.tracks.ActivityType
import moe.telecom.loclogger.ui.screen.tracks.TrackItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    val tracks: StateFlow<List<TrackItem>> = trackingRepository.getAllTracks()
        .map { entities -> entities.map { it.toTrackItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteTrack(item: TrackItem) {
        viewModelScope.launch {
            trackingRepository.getTrackById(item.id)?.let {
                trackingRepository.deleteTrack(it)
            }
        }
    }

    fun renameTrack(item: TrackItem, newName: String) {
        viewModelScope.launch {
            trackingRepository.renameTrack(item.id, newName)
        }
    }

    fun updateActivityType(item: TrackItem, type: ActivityType) {
        viewModelScope.launch {
            trackingRepository.updateActivityType(item.id, type.ordinal)
        }
    }

    /** 导出轨迹到临时文件并返回分享 Intent */
    suspend fun shareTrack(item: TrackItem, format: String): Intent {
        val fmt = format.lowercase()
        val track = trackingRepository.getTrackById(item.id)
            ?: throw IllegalStateException("轨迹不存在")
        val points = trackingRepository.getPointsForTrackSync(item.id)
        val annotations = trackingRepository.getAnnotationsForTrackSync(item.id)

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val fileName = trackingRepository.getExportFileName(track, fmt)
        val gpxVersion = settingsRepository.settings.first().gpxVersion
        val file = File(exportDir, fileName)
        file.outputStream().use { out ->
            trackingRepository.exportTrack(out, track, points, annotations, fmt, gpxVersion)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = trackingRepository.getExportMimeType(fmt)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun TrackEntity.toTrackItem(): TrackItem {
        val durationMs = (endTime ?: System.currentTimeMillis()) - startTime
        val activityType = ActivityType.entries.getOrElse(activityType) { ActivityType.WALK }
        val distanceStr = if (totalDistance >= 1000)
            String.format("%.2f 公里", totalDistance / 1000)
        else
            String.format("%.0f 米", totalDistance)
        val avgSpeedStr = String.format("%.1f km/h", avgSpeed * 3.6)

        return TrackItem(
            id = id,
            name = name,
            activityType = activityType,
            date = dateFormat.format(Date(startTime)),
            distance = distanceStr,
            duration = formatDuration(durationMs),
            avgSpeed = avgSpeedStr
        )
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}

data class TrackDetail(
    val track: TrackEntity,
    val points: List<moe.telecom.loclogger.data.local.entity.TrackPointEntity>,
    val annotations: List<moe.telecom.loclogger.data.local.entity.AnnotationEntity>
)