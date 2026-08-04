package moe.telecom.loclogger.ui.util

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器 - 适配国产 ROM
 * 支持：小米/红米、华为、OPPO、vivo、三星等
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 基础权限
    val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Android 10+ 后台定位
    val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        Manifest.permission.ACCESS_BACKGROUND_LOCATION else null

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocation(): Boolean =
        backgroundLocationPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    fun hasRequiredPermissions(): Boolean {
        val base = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return base && hasBackgroundLocation()
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 跳转到应用详情设置页
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    /**
     * 跳转到位置权限设置
     */
    fun openLocationSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    /**
     * 请求忽略电池优化
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (_: Exception) {
                openBatteryOptimizationSettings(activity)
            }
        }
    }

    /**
     * 跳转到电池优化设置
     */
    fun openBatteryOptimizationSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }

    /**
     * 跳转到国产 ROM 自启动设置
     * 按优先级尝试各厂商的自启动管理页面
     */
    fun openAutoStartSettings(activity: Activity) {
        val intents = listOf(
            // 小米/红米 - MIUI/HyperOS
            Intent().setComponent(ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartAlertActivity"
            )),
            // 华为/荣耀
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            )),
            // OPPO/一加/realme - ColorOS
            Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.oplus.safecenter",
                "com.oplus.safecenter.permission.startup.StartupAppListActivity"
            )),
            // vivo - OriginOS/FuntouchOS
            Intent().setComponent(ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )),
            // 三星
            Intent().setComponent(ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )),
            Intent().setComponent(ComponentName(
                "com.samsung.android.sm",
                "com.samsung.android.sm.ui.ram.AutoRunActivity"
            )),
            // 魅族 - Flyme
            Intent().setComponent(ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.permission.SmartBGActivity"
            )),
            // 联想/摩托罗拉
            Intent().setComponent(ComponentName(
                "com.lenovo.security",
                "com.lenovo.security.purebackground.PureBackgroundActivity"
            )),
        )

        intents.firstOrNull { intent ->
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }?.let {
            try {
                activity.startActivity(it)
                return
            } catch (_: Exception) { /* 继续尝试下一个 */ }
        }

        // 都找不到就跳应用详情
        openAppSettings(activity)
    }

    /**
     * 跳转到通知设置
     */
    fun openNotificationSettings(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    companion object {
        const val REQUEST_FINE_LOCATION = 1001
        const val REQUEST_BACKGROUND_LOCATION = 1002
        const val REQUEST_NOTIFICATIONS = 1003
        const val REQUEST_APP_SETTINGS = 1004
        const val REQUEST_BATTERY_OPTIMIZATION = 1005
        const val REQUEST_AUTO_START = 1006
    }
}
