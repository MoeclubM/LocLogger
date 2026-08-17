package moe.telecom.loclogger

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.telecom.loclogger.ui.LocalGlassBackdrop
import moe.telecom.loclogger.ui.LocalBottomBarInset
import moe.telecom.loclogger.ui.LocalMainPagerState
import moe.telecom.loclogger.ui.component.bottombar.BottomBar
import moe.telecom.loclogger.ui.component.bottombar.SideRail
import moe.telecom.loclogger.ui.component.bottombar.rememberMainPagerState
import moe.telecom.loclogger.ui.screen.dashboard.DashboardScreen
import moe.telecom.loclogger.ui.screen.settings.SettingsScreen
import moe.telecom.loclogger.ui.screen.track.TrackDetailScreen
import moe.telecom.loclogger.ui.screen.track.TrackScreen
import moe.telecom.loclogger.ui.screen.tracks.TrackItem
import moe.telecom.loclogger.ui.screen.tracks.TracksScreen
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
import moe.telecom.loclogger.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
                dynamicColor = settings.dynamicColor,
                pureBlack = settings.pureBlack
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
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
    var selectedTrack by remember { mutableStateOf<TrackItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val mainPagerState = rememberMainPagerState(pagerState, coroutineScope)

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

    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        mainPagerState.syncPage()
    }

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalGlassBackdrop provides (if (isMiuix) backdrop else null),
        LocalBottomBarInset provides if (enableFloatingBottomBar) 68.dp else 64.dp
    ) {
        Scaffold(
            bottomBar = {}
        ) { innerPadding ->

            // 宽屏/横屏时使用侧边导航栏（对齐 SukiSU showSplitPane 判定），Miuix 浮动底栏模式仍保留底部 Liquid Glass
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.toFloat()
            val screenHeightDp = configuration.screenHeightDp.toFloat()
            val showSplitPane = screenWidthDp >= 840f || (screenWidthDp >= 600f && screenHeightDp / screenWidthDp < 1.2f)
            val useNavigationRail = showSplitPane && !(isMiuix && enableFloatingBottomBar)

            val contentArea: @Composable () -> Unit = {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = pagerState.currentPage != 0 && selectedTrack == null,
                    modifier = Modifier.fillMaxSize()
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

            if (useNavigationRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SideRail(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(if (isMiuix) Modifier.layerBackdrop(backdrop) else Modifier)
                            .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            contentArea()
                        }
                    }
                }
            } else {
                // 毛玻璃 backdrop 只捕获页面内容（整屏含底栏后方），底栏作为捕获层之外的兄弟节点采样，
                Box(modifier = Modifier.fillMaxSize()) {
                    // 页面内容捕获层（整屏，底栏在层外）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isMiuix) Modifier.layerBackdrop(backdrop) else Modifier)
                            .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            contentArea()
                        }
                    }
                    // 底部栏覆盖在内容之上（不占内边距），内容延伸到其后方供毛玻璃采样
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
