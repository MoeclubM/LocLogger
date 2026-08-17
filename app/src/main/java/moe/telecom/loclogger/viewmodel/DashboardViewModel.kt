package moe.telecom.loclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val bearing: Float? = null,
    val satellitesUsed: Int = 0,
    val satellitesVisible: Int = 0,
    val time: String? = null,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val trackPoints: Int = 0,
    val distance: Double = 0.0,
    val duration: Long = 0L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val timeFormat = SimpleDateFormat("HH:mm:ss (z)", Locale.getDefault())

    val uiState: StateFlow<DashboardUiState> = trackingRepository.trackingState
        .map { state ->
            DashboardUiState(
                latitude = state.latitude,
                longitude = state.longitude,
                altitude = state.altitude,
                speed = state.speed,
                accuracy = state.accuracy,
                bearing = state.bearing,
                satellitesUsed = state.satellitesUsed,
                satellitesVisible = state.satellitesVisible,
                time = state.lastUpdateTime.takeIf { it > 0 }?.let { timeFormat.format(Date(it)) },
                isRecording = state.isRecording,
                isPaused = state.isPaused,
                trackPoints = state.pointCount,
                distance = state.distance,
                duration = state.duration
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    init {
        trackingRepository.bindService()
    }
}
