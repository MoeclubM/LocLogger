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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors

/**
 * 权限请求状态
 */
sealed class PermissionState {
    data object Granted : PermissionState()
    data class ShowRationale(val permission: String) : PermissionState()
    data class NeedSettings(val reason: String) : PermissionState()
    data class Denied(val message: String) : PermissionState()
}

/**
 * 权限请求 Composable
 *
 * 处理流程：
 * 1. 检查权限 -> 已授权 -> 直接执行 onGranted
 * 2. 未授权 -> 显示说明对话框 -> 用户同意 -> 请求权限
 * 3. 用户授权 -> 自动重试 onGranted（关键：授权后再调用一次）
 * 4. 用户拒绝 -> 显示拒绝提示
 * 5. 用户选"不再询问" -> 引导跳设置页
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
    var showSettingsDialog by remember { mutableStateOf<String?>(null) }
    var pendingRequest by remember { mutableStateOf(false) }

    fun checkFinalResult() {
        if (permissionManager.hasRequiredPermissions()) {
            onPermissionResult(true)
        } else {
            onPermissionResult(false)
        }
    }

    // 通知权限请求
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // 通知权限不阻塞主流程，检查最终结果
        checkFinalResult()
    }

    // 从设置页返回
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 从设置返回后重新检查
        checkFinalResult()
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!permissionManager.hasNotificationPermission()) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkFinalResult()
    }

    // 后台定位权限请求
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestNotificationPermission()
        } else {
            // 后台定位被拒，提示但不阻塞（前台定位已授权也能用）
            requestNotificationPermission()
        }
    }

    // 前台定位权限请求
    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 前台定位授权后，继续请求后台定位
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // Android 9 及以下没有后台定位权限，直接请求通知
                requestNotificationPermission()
            }
        } else {
            if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // 用户选了"不再询问"
                showSettingsDialog = "定位权限被拒绝且不再询问，请在设置中手动开启"
            } else {
                showRationaleDialog = Manifest.permission.ACCESS_FINE_LOCATION
            }
            onPermissionResult(false)
        }
    }

    fun startPermissionFlow() {
        when {
            permissionManager.hasRequiredPermissions() -> {
                onPermissionResult(true)
            }
            !permissionManager.hasFineLocation() -> {
                if (activity != null && activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showRationaleDialog = Manifest.permission.ACCESS_FINE_LOCATION
                } else {
                    fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            !permissionManager.hasBackgroundLocation() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (activity != null && activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        showRationaleDialog = Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    } else {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }
            }
            else -> {
                requestNotificationPermission()
            }
        }
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
                        fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    // 引导去设置对话框
    showSettingsDialog?.let { reason ->
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = null
                onPermissionResult(false)
            },
            title = { Text("需要手动开启权限") },
            text = { Text(reason) },
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
                    onPermissionResult(false)
                }) {
                    Text("取消")
                }
            }
        )
    }

    content { startPermissionFlow() }
}

/**
 * 权限状态卡片 - 显示当前权限状态
 */
@Composable
fun PermissionStatusCard(
    permissionManager: PermissionManager,
    onRequestPermissions: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onRequestBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLocation = permissionManager.hasFineLocation()
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
                subtitle = "息屏时持续记录",
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

            OutlinedButton(
                onClick = onRequestBattery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("加入电池优化白名单")
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
