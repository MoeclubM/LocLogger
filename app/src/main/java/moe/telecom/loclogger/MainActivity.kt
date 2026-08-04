package moe.telecom.loclogger

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
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
import moe.telecom.loclogger.ui.screen.track.TrackDetailScreen
import moe.telecom.loclogger.ui.screen.tracks.TrackItem
import moe.telecom.loclogger.ui.theme.ColorMode
import moe.telecom.loclogger.ui.theme.GpsLoggerTheme
import moe.telecom.loclogger.ui.theme.LocalEnableBlur
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBar
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBarBlur
import moe.telecom.loclogger.ui.theme.LocalUiMode
import moe.telecom.loclogger.ui.theme.UiMode
import moe.telecom.loclogger.ui.theme.keyColorOptions
import moe.telecom.loclogger.ui.util.PermissionManager
import moe.telecom.loclogger.ui.util.PermissionRequester
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
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

                selectedTrack?.let { track ->
                    TrackDetailScreen(
                        trackItem = track,
                        onBack = { selectedTrack = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    var selectedTrack by remember { mutableStateOf<TrackItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val mainPagerState = rememberMainPagerState(pagerState, coroutineScope)

    // Liquid Glass（参考 SukiSU）：双 backdrop 捕获
    //  - blurBackdrop：普通导航栏毛玻璃（textureBlur），仅启用全局模糊时创建
    //  - backdrop：浮动底栏 Liquid Glass 折射/模糊（drawBackdrop），浮动+模糊开启时捕获页面
    val uiMode = LocalUiMode.current
    val isMiuix = uiMode == UiMode.Miuix
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current

    val surfaceColor = if (isMiuix) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val blurBackdrop = if (enableBlur && isRenderEffectSupported()) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // 同步页面状态
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        mainPagerState.syncPage()
    }

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        Scaffold(
            bottomBar = { BottomBar(blurBackdrop = blurBackdrop, backdrop = backdrop) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = pagerState.currentPage != 0 && selectedTrack == null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isMiuix) Modifier.layerBackdrop(backdrop) else Modifier)
                ) { page ->
                    when (page) {
                        0 -> DashboardScreen()
                        1 -> TrackScreen()
                        2 -> TracksScreen(onTrackClick = { selectedTrack = it })
                        3 -> SettingsScreen()
                    }
                }

                selectedTrack?.let { track ->
                    TrackDetailScreen(
                        trackItem = track,
                        onBack = { selectedTrack = null }
                    )
                }
            }
        }
    }
}
