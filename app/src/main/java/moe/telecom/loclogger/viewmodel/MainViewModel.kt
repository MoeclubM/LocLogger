package moe.telecom.loclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettings(
    val uiMode: Int = 0,
    val colorMode: Int = 0,
    val themeColorIndex: Int = 0,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    val keepScreenOn: Boolean = false,
    val unitMetric: Boolean = true,
    val speedUnit: Int = 0,
    val coordFormat: Int = 0,
    val gpsInterval: Int = 1000,
    val timeFilter: Int = 0,
    val distanceFilter: Float = 0f,
    val improveAccuracy: Boolean = true,
    val egm96Correction: Boolean = false,
    val exportFormats: Set<String> = setOf("gpx"),
    val gpxVersion: String = "1.1"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateUiMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.updateUiMode(mode)
        }
    }

    fun updateColorMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.updateColorMode(mode)
        }
    }

    fun updateThemeColor(index: Int) {
        viewModelScope.launch {
            settingsRepository.updateThemeColor(index)
        }
    }

    fun updateEnableBlur(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableBlur(enabled)
        }
    }

    fun updateFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFloatingBottomBar(enabled)
        }
    }

    fun updateFloatingBottomBarBlur(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFloatingBottomBarBlur(enabled)
        }
    }
}
