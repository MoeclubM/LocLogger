package moe.telecom.loclogger.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.telecom.loclogger.ui.LocalBottomBarInset
import moe.telecom.loclogger.ui.theme.ColorMode
import moe.telecom.loclogger.ui.theme.UiMode
import moe.telecom.loclogger.ui.theme.isInDarkTheme
import moe.telecom.loclogger.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeManagerScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题管理器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(
                title = "主题",
                icon = Icons.Default.Palette
            ) {
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
                    title = "模糊效果",
                    subtitle = "界面毛玻璃/透镜模糊",
                    checked = settings.enableBlur,
                    onCheckedChange = { viewModel.updateEnableBlur(it) }
                )
                SettingsSwitchItem(
                    title = "动态取色 (Material You)",
                    subtitle = "跟随系统壁纸自动取色",
                    checked = settings.dynamicColor,
                    onCheckedChange = { viewModel.updateDynamicColor(it) }
                )
                if (isInDarkTheme()) {
                    SettingsSwitchItem(
                        title = "纯黑模式 (AMOLED)",
                        subtitle = "深色下使用纯黑背景",
                        checked = settings.pureBlack,
                        onCheckedChange = { viewModel.updatePureBlack(it) }
                    )
                }
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

            Spacer(modifier = Modifier.height(16.dp + LocalBottomBarInset.current))
        }
    }
}
