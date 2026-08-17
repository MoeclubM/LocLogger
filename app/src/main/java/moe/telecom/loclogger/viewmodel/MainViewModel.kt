package moe.telecom.loclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.telecom.loclogger.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppSettings(
    val uiMode: Int = 0,
    val colorMode: Int = 0,
    val themeColorIndex: Int = 0,
    val enableBlur: Boolean = true,
    val dynamicColor: Boolean = false,
    val pureBlack: Boolean = false,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    val keepScreenOn: Boolean = false,
    val gpsInterval: Int = 1000,
    val improveAccuracy: Boolean = true,
    val egm96Correction: Boolean = false,
    val gpxVersion: String = "1.1"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
}
