package moe.telecom.loclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TrackUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isLocked: Boolean = false,
    val trackName: String = "",
    val pointCount: Int = 0,
    val annotationCount: Int = 0,
    val duration: Long = 0L,
    val distance: Double = 0.0,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val altitudeDiff: Double = 0.0,
    val overallDirection: String? = null
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _isLocked = MutableStateFlow(false)

    val uiState: StateFlow<TrackUiState> = combine(
        trackingRepository.trackingState,
        _isLocked
    ) { state, locked ->
        TrackUiState(
            isRecording = state.isRecording,
            isPaused = state.isPaused,
            isLocked = locked,
            trackName = state.trackName,
            pointCount = state.pointCount,
            annotationCount = state.annotationCount,
            duration = state.duration,
            distance = state.distance,
            maxSpeed = state.maxSpeed,
            avgSpeed = state.avgSpeed,
            altitudeDiff = state.altitudeDiff,
            overallDirection = state.bearing?.let { bearingToDirection(it) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrackUiState()
    )

    fun start() = trackingRepository.startTracking()

    fun pause() = trackingRepository.pauseTracking()
    fun resume() = trackingRepository.resumeTracking()
    fun stop() = trackingRepository.stopTracking()

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun addAnnotation(description: String = "批注") {
        trackingRepository.addAnnotation(description)
    }

    private fun bearingToDirection(bearing: Float): String {
        val directions = arrayOf(
            "北", "北偏东", "东北偏北", "东北", "东北偏东", "东偏北",
            "东", "东偏南", "东南偏东", "东南", "东南偏南", "南偏东",
            "南", "南偏西", "西南偏南", "西南", "西南偏西", "西偏南",
            "西", "西偏北", "西北偏西", "西北", "西北偏北", "北偏西"
        )
        val index = ((bearing / 15f) + 0.5f).toInt() % 24
        return directions[index]
    }
}
