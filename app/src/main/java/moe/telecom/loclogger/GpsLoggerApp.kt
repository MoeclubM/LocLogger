package moe.telecom.loclogger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class GpsLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 设备蜂窝网络可能是 IPv6-only（IPv4 被阻断/污染）：
        // 让 Java 的 InetAddress 优先解析 IPv6，避免部分网络下瓦片/资源请求挂起。
        System.setProperty("java.net.preferIPv6Addresses", "true")
        // 初始化 MapLibre
        MapLibre.getInstance(this)
    }
}
