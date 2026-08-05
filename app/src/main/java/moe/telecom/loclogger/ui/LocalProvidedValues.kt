package moe.telecom.loclogger.ui

import androidx.compose.runtime.staticCompositionLocalOf
import moe.telecom.loclogger.ui.component.bottombar.MainPagerState
import top.yukonga.miuix.kmp.blur.LayerBackdrop

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }