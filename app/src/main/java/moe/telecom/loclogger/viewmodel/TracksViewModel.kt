package moe.telecom.loclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.repository.TrackingRepository
import moe.telecom.loclogger.ui.screen.tracks.ActivityType
import moe.telecom.loclogger.ui.screen.tracks.TrackItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
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
