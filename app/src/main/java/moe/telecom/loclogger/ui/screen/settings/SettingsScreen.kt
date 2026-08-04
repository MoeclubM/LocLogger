package moe.telecom.loclogger.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import moe.telecom.loclogger.ui.theme.ColorMode
import moe.telecom.loclogger.ui.theme.UiMode
import moe.telecom.loclogger.ui.theme.keyColorOptions
import moe.telecom.loclogger.ui.util.PermissionEntryPoint
import moe.telecom.loclogger.ui.util.PermissionManager
import moe.telecom.loclogger.ui.util.PermissionStatusCard
import moe.telecom.loclogger.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val permissionManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PermissionEntryPoint::class.java
        ).permissionManager()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 结果无需额外处理，PermissionStatusCard 会随重组刷新状态 */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 显示设置
        SettingsSection(
            title = "显示",
            icon = Icons.Default.DisplaySettings
        ) {
            SettingsSwitchItem(
                title = "保持屏幕常亮",
                subtitle = "记录时保持屏幕开启",
                checked = settings.keepScreenOn,
                onCheckedChange = { viewModel.updateKeepScreenOn(it) }
            )
            SettingsDropdownItem(
                title = "当前主题",
                subtitle = UiMode.fromInt(settings.uiMode).displayName,
                options = UiMode.entries.map { it.displayName },
                selectedIndex = UiMode.fromInt(settings.uiMode).ordinal,
                onSelected = { viewModel.updateUiMode(it) }
            )
            SettingsDropdownItem(
                title = "颜色模式",
                subtitle = ColorMode.fromInt(settings.colorMode).displayName,
                options = ColorMode.entries.map { it.displayName },
                selectedIndex = settings.colorMode,
                onSelected = { viewModel.updateColorMode(it) }
            )
            ColorPickerItem(
                title = "主题色",
                selectedIndex = settings.themeColorIndex,
                onSelected = { viewModel.updateThemeColor(it) }
            )
            SettingsSwitchItem(
                title = "Liquid Glass 底栏",
                subtitle = "浮动式毛玻璃导航栏",
                checked = settings.enableFloatingBottomBar,
                onCheckedChange = { viewModel.updateFloatingBottomBar(it) }
            )
            if (settings.enableFloatingBottomBar) {
                SettingsSwitchItem(
                    title = "底栏模糊效果",
                    subtitle = "开启毛玻璃/透镜折射",
                    checked = settings.enableFloatingBottomBarBlur,
                    onCheckedChange = { viewModel.updateFloatingBottomBarBlur(it) }
                )
            }
        }

        // 记录设置
        SettingsSection(
            title = "记录",
            icon = Icons.Default.FiberManualRecord
        ) {
            SettingsDropdownItem(
                title = "GPS 更新周期",
                subtitle = when (settings.gpsInterval) {
                    500 -> "500 毫秒"
                    1000 -> "1 秒"
                    2000 -> "2 秒"
                    5000 -> "5 秒"
                    else -> "${settings.gpsInterval} 毫秒"
                },
                options = listOf("500 毫秒", "1 秒", "2 秒", "5 秒"),
                selectedIndex = when (settings.gpsInterval) {
                    500 -> 0; 1000 -> 1; 2000 -> 2; 5000 -> 3; else -> 1
                },
                onSelected = {
                    viewModel.updateGpsInterval(
                        when (it) { 0 -> 500; 1 -> 1000; 2 -> 2000; else -> 5000 }
                    )
                }
            )
            SettingsSwitchItem(
                title = "提高 GPS 精度",
                subtitle = "使用网络定位辅助",
                checked = settings.improveAccuracy,
                onCheckedChange = { viewModel.updateImproveAccuracy(it) }
            )
            SettingsSwitchItem(
                title = "EGM96 高度修正",
                subtitle = "修正海拔高度（需要数据文件）",
                checked = settings.egm96Correction,
                onCheckedChange = { viewModel.updateEgm96(it) }
            )
        }

        // 导出设置
        SettingsSection(
            title = "导出",
            icon = Icons.Default.Download
        ) {
            SettingsDropdownItem(
                title = "GPX 版本",
                subtitle = settings.gpxVersion,
                options = listOf("1.0", "1.1", "2.2"),
                selectedIndex = when (settings.gpxVersion) {
                    "1.0" -> 0; "2.2" -> 2; else -> 1
                },
                onSelected = {
                    viewModel.updateGpxVersion(
                        when (it) { 0 -> "1.0"; 2 -> "2.2"; else -> "1.1" }
                    )
                }
            )
        }

        // 权限与保活
        SettingsSection(
            title = "权限与保活",
            icon = Icons.Default.Security
        ) {
            PermissionStatusCard(
                permissionManager = permissionManager,
                onRequestPermissions = {
                    // 前台定位 + 通知：FINE/COARSE 必须同时请求；BACKGROUND 不能混在数组里（Android 11+ 会被忽略）
                    permissionLauncher.launch(
                        buildList {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                    )
                },
                onOpenBackgroundLocation = { activity?.let { permissionManager.openAppSettings(it) } },
                onOpenAutoStart = { activity?.let { permissionManager.openAutoStartSettings(it) } },
                onRequestBattery = { activity?.let { permissionManager.requestIgnoreBatteryOptimizations(it) } }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LocLogger v1.0.0",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            Text(
                text = options[selectedIndex],
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerItem(
    title: String,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Box {
            // 颜色圆点（点击弹出主题色选择）
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = Color(keyColorOptions[selectedIndex])
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
            ) {}
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                keyColorOptions.forEachIndexed { index, color ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(color), RoundedCornerShape(50))
                                )
                                Text("主题色 ${index + 1}")
                            }
                        },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
