package moe.telecom.loclogger.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import moe.telecom.loclogger.viewmodel.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val UI_MODE = intPreferencesKey("ui_mode")
        val COLOR_MODE = intPreferencesKey("color_mode")
        val THEME_COLOR = intPreferencesKey("theme_color_index")
        val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
        val FLOATING_BAR = booleanPreferencesKey("floating_bottom_bar")
        val FLOATING_BAR_BLUR = booleanPreferencesKey("floating_bottom_bar_blur")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val UNIT_METRIC = booleanPreferencesKey("unit_metric")
        val SPEED_UNIT = intPreferencesKey("speed_unit")
        val COORD_FORMAT = intPreferencesKey("coord_format")
        val GPS_INTERVAL = intPreferencesKey("gps_interval")
        val TIME_FILTER = intPreferencesKey("time_filter")
        val DISTANCE_FILTER = floatPreferencesKey("distance_filter")
        val IMPROVE_ACCURACY = booleanPreferencesKey("improve_accuracy")
        val EGM96 = booleanPreferencesKey("egm96_correction")
        val EXPORT_FORMATS = stringSetPreferencesKey("export_formats")
        val GPX_VERSION = intPreferencesKey("gpx_version")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            uiMode = prefs[Keys.UI_MODE] ?: 0,
            colorMode = prefs[Keys.COLOR_MODE] ?: 0,
            themeColorIndex = prefs[Keys.THEME_COLOR] ?: 0,
            enableBlur = prefs[Keys.ENABLE_BLUR] ?: true,
            enableFloatingBottomBar = prefs[Keys.FLOATING_BAR] ?: true,
            enableFloatingBottomBarBlur = prefs[Keys.FLOATING_BAR_BLUR] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            unitMetric = prefs[Keys.UNIT_METRIC] ?: true,
            speedUnit = prefs[Keys.SPEED_UNIT] ?: 0,
            coordFormat = prefs[Keys.COORD_FORMAT] ?: 0,
            gpsInterval = prefs[Keys.GPS_INTERVAL] ?: 1000,
            timeFilter = prefs[Keys.TIME_FILTER] ?: 0,
            distanceFilter = prefs[Keys.DISTANCE_FILTER] ?: 0f,
            improveAccuracy = prefs[Keys.IMPROVE_ACCURACY] ?: true,
            egm96Correction = prefs[Keys.EGM96] ?: false,
            exportFormats = prefs[Keys.EXPORT_FORMATS] ?: setOf("gpx"),
            gpxVersion = when (prefs[Keys.GPX_VERSION] ?: 1) {
                0 -> "1.0"
                2 -> "2.2"
                else -> "1.1"
            }
        )
    }

    suspend fun updateUiMode(mode: Int) {
        context.dataStore.edit { it[Keys.UI_MODE] = mode }
    }

    suspend fun updateColorMode(mode: Int) {
        context.dataStore.edit { it[Keys.COLOR_MODE] = mode }
    }

    suspend fun updateThemeColor(index: Int) {
        context.dataStore.edit { it[Keys.THEME_COLOR] = index }
    }

    suspend fun updateEnableBlur(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_BLUR] = enabled }
    }

    suspend fun updateFloatingBottomBar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_BAR] = enabled }
    }

    suspend fun updateFloatingBottomBarBlur(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_BAR_BLUR] = enabled }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateGpsInterval(interval: Int) {
        context.dataStore.edit { it[Keys.GPS_INTERVAL] = interval }
    }

    suspend fun updateImproveAccuracy(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IMPROVE_ACCURACY] = enabled }
    }

    suspend fun updateEgm96(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EGM96] = enabled }
    }

    suspend fun updateGpxVersion(version: String) {
        context.dataStore.edit {
            it[Keys.GPX_VERSION] = when (version) {
                "1.0" -> 0
                "2.2" -> 2
                else -> 1
            }
        }
    }
}
