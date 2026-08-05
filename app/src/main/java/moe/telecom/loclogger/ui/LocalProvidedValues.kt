package moe.telecom.loclogger.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.ui.component.bottombar.MainPagerState
import top.yukonga.miuix.kmp.blur.LayerBackdrop

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

// 底部栏在导航栏 inset 之上的高度；内容延伸到底栏后方，各页尾部据此留白避免被遮挡
val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }
