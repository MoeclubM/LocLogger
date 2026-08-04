package moe.telecom.loclogger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class GpsLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 osmdroid
        Configuration.getInstance().apply {
            userAgentValue = packageName
            tileFileSystemCacheMaxBytes = 500L * 1024 * 1024 // 500MB 瓦片缓存
            // TODO(debug): 定位瓦片不加载问题后需改回 false
            isDebugMode = true
            isDebugTileProviders = true
        }
    }
}
