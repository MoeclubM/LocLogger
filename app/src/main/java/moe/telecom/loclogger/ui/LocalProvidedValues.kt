package moe.telecom.loclogger.ui

import androidx.compose.runtime.staticCompositionLocalOf
import moe.telecom.loclogger.ui.component.bottombar.MainPagerState

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}
