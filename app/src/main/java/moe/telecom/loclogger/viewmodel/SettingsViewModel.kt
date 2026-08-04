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

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            // TODO: 保存到 DataStore
        }
    }

    fun updateFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateFloatingBottomBar(enabled) }
    }

    fun updateFloatingBottomBarBlur(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateFloatingBottomBarBlur(enabled) }
    }

    fun updateGpsInterval(interval: Int) {
        viewModelScope.launch {
            // TODO: 保存到 DataStore
        }
    }

    fun updateImproveAccuracy(enabled: Boolean) {
        viewModelScope.launch {
            // TODO: 保存到 DataStore
        }
    }

    fun updateEgm96(enabled: Boolean) {
        viewModelScope.launch {
            // TODO: 保存到 DataStore
        }
    }

    fun updateGpxVersion(version: String) {
        viewModelScope.launch {
            // TODO: 保存到 DataStore
        }
    }
}
