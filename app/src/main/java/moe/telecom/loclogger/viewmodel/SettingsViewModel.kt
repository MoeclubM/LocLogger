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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateUiMode(mode: Int) {
        viewModelScope.launch { settingsRepository.updateUiMode(mode) }
    }

    fun updateColorMode(mode: Int) {
        viewModelScope.launch { settingsRepository.updateColorMode(mode) }
    }

    fun updateThemeColor(index: Int) {
        viewModelScope.launch { settingsRepository.updateThemeColor(index) }
    }

    fun updateEnableBlur(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableBlur(enabled) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateKeepScreenOn(enabled) }
    }

    fun updateFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateFloatingBottomBar(enabled) }
    }

    fun updateFloatingBottomBarBlur(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateFloatingBottomBarBlur(enabled) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicColor(enabled) }
    }

    fun updatePureBlack(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updatePureBlack(enabled) }
    }

    fun updateGpsInterval(interval: Int) {
        viewModelScope.launch { settingsRepository.updateGpsInterval(interval) }
    }

    fun updateImproveAccuracy(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateImproveAccuracy(enabled) }
    }

    fun updateEgm96(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEgm96(enabled) }
    }

    fun updateGpxVersion(version: String) {
        viewModelScope.launch { settingsRepository.updateGpxVersion(version) }
    }
}
