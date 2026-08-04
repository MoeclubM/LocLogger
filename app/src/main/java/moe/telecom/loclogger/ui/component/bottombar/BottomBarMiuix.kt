package moe.telecom.loclogger.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.R
import moe.telecom.loclogger.ui.LocalMainPagerState
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

// GPS Logger 底部导航项
enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Dashboard(R.string.tab_dashboard, Icons.Rounded.Explore),
    Track(R.string.tab_track, Icons.Rounded.Timeline),
    Tracks(R.string.tab_tracks, Icons.Rounded.Map),
    Settings(R.string.tab_settings, Icons.Rounded.Settings)
}

@Composable
fun BottomBarMiuix(
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val items = BottomBarDestination.entries

    if (!enableFloatingBottomBar) {
        // 普通导航栏模式
        NavigationBar(
            modifier = modifier,
            color = MiuixTheme.colorScheme.surface,
        ) {
            items.forEachIndexed { index, destination ->
                NavigationBarItem(
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    selected = mainState.selectedPage == index,
                    onClick = { mainState.animateToPage(index) },
                    icon = destination.icon,
                    label = stringResource(destination.label)
                )
            }
        }
    } else {
        // Liquid Glass 浮动导航栏模式
        FloatingNavigationBar(
            modifier = modifier.padding(
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            color = MiuixTheme.colorScheme.surfaceContainer,
        ) {
            items.forEachIndexed { index, destination ->
                FloatingNavigationBarItem(
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    selected = mainState.selectedPage == index,
                    onClick = { mainState.animateToPage(index) },
                    icon = destination.icon,
                    label = stringResource(destination.label)
                )
            }
        }
    }
}
