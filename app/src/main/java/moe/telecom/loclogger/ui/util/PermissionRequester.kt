package moe.telecom.loclogger.ui.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 设置页引导弹窗内容；blocking=true 时拒绝会阻塞主流程，false 时仅提示不阻塞 */
private data class SettingsPrompt(val reason: String, val blocking: Boolean)

/**
 * 权限请求 Composable
 *
 * 处理流程：
 * 1. 已授权 -> 回调 onPermissionResult(true)
 * 2. 前台定位未授权 -> 同时请求 FINE + COARSE（Android 12+ 官方要求，单独请求 FINE 会被忽略）
 * 3. 后台定位：
 *    - Android 11+ 系统弹窗不再提供"始终允许"，引导去系统设置开启（不阻塞主流程）
 *    - Android 10 及以下直接请求（系统弹窗含"始终允许"）
 * 4. 通知权限 -> 请求
 */
@Composable
fun PermissionRequester(
    permissionManager: PermissionManager,
    onPermissionResult: (Boolean) -> Unit = {},
    content: @Composable (requestPermissions: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showRationaleDialog by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf<SettingsPrompt?>(null) }

    fun checkFinalResult() {
        onPermissionResult(permissionManager.hasRequiredPermissions())
    }

    // 通知权限请求（Android 13+）
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkFinalResult()
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissionManager.hasNotificationPermission()
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkFinalResult()
        }
    }

    // 后台定位请求（仅 Android 10 及以下使用，系统弹窗含"始终允许"）
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestNotificationPermission()
    }

    // 后台定位：Android 11+ 只能去系统设置开启，Android 10 及以下直接请求
    fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showSettingsDialog = SettingsPrompt(
                "Android 11 及以上系统不再提供「始终允许」选项。\n\n" +
                    "请到系统设置中为 GPS Logger 开启「允许所有时间」定位权限，" +
                    "以便息屏或切换到其他应用后仍能持续记录轨迹。",
                blocking = false
            )
        } else if (activity != null &&
            activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            showRationaleDialog = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // 前台定位授予后的后续流程：后台引导 / 通知
    fun continueAfterForegroundLocation() {
        if (!permissionManager.hasBackgroundLocation()) {
            requestBackgroundLocation()
        } else {
            requestNotificationPermission()
        }
    }

    // 前台定位请求：FINE + COARSE 必须同时请求
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (permissionManager.hasForegroundLocation()) {
            // 前台定位已授予，继续后台引导 / 通知
            continueAfterForegroundLocation()
        } else {
            val fineDenied = result[Manifest.permission.ACCESS_FINE_LOCATION] == false
            if (fineDenied && activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                // 用户选了"不再询问"，引导去设置
                showSettingsDialog = SettingsPrompt(
                    "定位权限被拒绝且不再询问，请在设置中手动开启「精确位置」权限。",
                    blocking = true
                )
            } else {
                showRationaleDialog = Manifest.permission.ACCESS_FINE_LOCATION
            }
            onPermissionResult(false)
        }
    }

    fun startPermissionFlow() {
        when {
            // 全部就绪（含后台定位）才算完成；Android 11+ 后台缺失时走 else 不阻塞
            permissionManager.hasRequiredPermissions() && permissionManager.hasBackgroundLocation() ->
                onPermissionResult(true)
            !permissionManager.hasForegroundLocation() -> {
                if (activity != null &&
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
                    showRationaleDialog = Manifest.permission.ACCESS_FINE_LOCATION
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
            // Android 10 及以下后台定位缺失时自动请求（系统弹窗含"始终允许"）；
            // Android 11+ 不自动打扰，由前台授权回调或设置页引导
            !permissionManager.hasBackgroundLocation() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R ->
                requestBackgroundLocation()
            else -> requestNotificationPermission()
        }
    }

    // 从设置页返回后继续完整流程（后台定位/通知可能已手动开启）
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        startPermissionFlow()
    }

    // 权限说明对话框
    showRationaleDialog?.let { permission ->
        val isBackground = permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION
        AlertDialog(
            onDismissRequest = {
                showRationaleDialog = null
                onPermissionResult(false)
            },
            title = { Text(if (isBackground) "需要后台定位权限" else "需要定位权限") },
            text = {
                Text(
                    if (isBackground)
                        "为了在息屏或后台时持续记录轨迹，需要允许「始终允许」定位权限。\n\n如果不授予此权限，切换到其他应用或锁屏后记录可能中断。"
                    else
                        "GPS Logger 需要定位权限来获取位置信息并记录轨迹。\n\n请授予「精确位置」权限以获得最佳精度。"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = null
                    if (isBackground) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }) {
                    Text("授予权限")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationaleDialog = null
                    onPermissionResult(false)
                }) {
                    Text("稍后")
                }
            }
        )
    }

    // 引导去设置对话框（前台被拒不再询问 / Android 11+ 后台定位）
    showSettingsDialog?.let { prompt ->
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = null
                onPermissionResult(!prompt.blocking)
            },
            title = { Text(if (prompt.blocking) "需要手动开启权限" else "建议开启后台定位") },
            text = { Text(prompt.reason) },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog = null
                    activity?.let {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", it.packageName, null)
                        }
                        settingsLauncher.launch(intent)
                    }
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsDialog = null
                    onPermissionResult(!prompt.blocking)
                }) {
                    Text(if (prompt.blocking) "取消" else "稍后")
                }
            }
        )
    }

    // 进入组合后自动启动权限请求流程（仅首次组合执行一次）
    LaunchedEffect(Unit) {
        startPermissionFlow()
    }

    // 同时暴露手动触发入口（供 UI 按钮使用）
    content { startPermissionFlow() }
}

/**
 * Hilt EntryPoint：供非 Activity/ViewModel 场景（如设置页）获取 PermissionManager
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PermissionEntryPoint {
    fun permissionManager(): PermissionManager
}

/**
 * 权限状态卡片 - 显示当前权限状态
 */
@Composable
fun PermissionStatusCard(
    permissionManager: PermissionManager,
    onRequestPermissions: () -> Unit,
    onOpenBackgroundLocation: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onRequestBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLocation = permissionManager.hasForegroundLocation()
    val hasBackground = permissionManager.hasBackgroundLocation()
    val hasNotification = permissionManager.hasNotificationPermission()
    val hasBattery = permissionManager.isIgnoringBatteryOptimizations()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "权限状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = "精确定位",
                subtitle = "获取 GPS 位置",
                granted = hasLocation
            )

            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = "后台定位",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    "息屏持续记录（Android 11+ 需到系统设置开启）"
                else
                    "息屏时持续记录",
                granted = hasBackground
            )

            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "通知权限",
                subtitle = "显示记录状态通知",
                granted = hasNotification
            )

            PermissionItem(
                icon = Icons.Default.BatteryAlert,
                title = "电池优化白名单",
                subtitle = "防止系统杀后台",
                granted = hasBattery
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("请求权限")
                }
                OutlinedButton(
                    onClick = onOpenAutoStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("自启动设置")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenBackgroundLocation,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("后台定位设置")
                }
                OutlinedButton(
                    onClick = onRequestBattery,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("电池白名单")
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (granted) "✓ 已授权" else "✗ 未授权",
            fontSize = 12.sp,
            color = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}
