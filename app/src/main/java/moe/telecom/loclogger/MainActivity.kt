package moe.telecom.loclogger

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.telecom.loclogger.ui.LocalMainPagerState
import moe.telecom.loclogger.ui.component.bottombar.BottomBar
import moe.telecom.loclogger.ui.component.bottombar.rememberMainPagerState
import moe.telecom.loclogger.ui.screen.dashboard.DashboardScreen
import moe.telecom.loclogger.ui.screen.settings.SettingsScreen
import moe.telecom.loclogger.ui.screen.track.TrackScreen
import moe.telecom.loclogger.ui.screen.tracks.TracksScreen
import moe.telecom.loclogger.ui.theme.ColorMode
import moe.telecom.loclogger.ui.theme.GpsLoggerTheme
import moe.telecom.loclogger.ui.theme.LocalEnableBlur
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBarBlur
import moe.telecom.loclogger.ui.theme.LocalLayerBackdrop
import moe.telecom.loclogger.ui.theme.LocalUiMode
import moe.telecom.loclogger.ui.theme.UiMode
import moe.telecom.loclogger.ui.theme.keyColorOptions
import moe.telecom.loclogger.ui.util.PermissionManager
import moe.telecom.loclogger.ui.util.PermissionRequester
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import moe.telecom.loclogger.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settingsState.collectAsState()

            // 保持屏幕常亮
            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            GpsLoggerTheme(
                themeColor = keyColorOptions[settings.themeColorIndex],
                uiMode = UiMode.fromInt(settings.uiMode),
                colorMode = ColorMode.fromInt(settings.colorMode),
                enableBlur = settings.enableBlur,
                enableFloatingBottomBar = settings.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 启动时自动请求定位/通知权限（未授权才弹窗）
                    PermissionRequester(permissionManager = permissionManager) { _ ->
                        MainContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val mainPagerState = rememberMainPagerState(pagerState, coroutineScope)

    // Liquid Glass 底栏：捕获页面内容作为毛玻璃背景（先铺不透明背景避免透明区域扩散）
    val isMiuix = LocalUiMode.current == UiMode.Miuix
    val backdropBackground = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backdropBackground)
        drawContent()
    }
    // 参考 SukiSU：仅启用模糊时捕获页面内容，避免无谓渲染开销
    val captureBackdrop = isMiuix && (LocalEnableBlur.current || LocalEnableFloatingBottomBarBlur.current)

    // 同步页面状态
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        mainPagerState.syncPage()
    }

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalLayerBackdrop provides backdrop
    ) {
        Scaffold(
            bottomBar = { BottomBar() }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (captureBackdrop) Modifier.layerBackdrop(backdrop) else Modifier)
            ) { page ->
                when (page) {
                    0 -> DashboardScreen()
                    1 -> TrackScreen()
                    2 -> TracksScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }
}
