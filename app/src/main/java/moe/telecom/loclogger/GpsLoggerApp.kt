package moe.telecom.loclogger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class GpsLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 设备蜂窝网络可能是 IPv6-only（IPv4 被阻断/污染）：
        // 让 Java 的 InetAddress 优先解析 IPv6，否则 osmdroid 瓦片下载会走 IPv4 挂起。
        // Android libcore 支持此属性；IPv4-only 网络会自动回退，双栈安全。
        System.setProperty("java.net.preferIPv6Addresses", "true")
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
