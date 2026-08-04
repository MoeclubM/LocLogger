package moe.telecom.loclogger.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.R
import moe.telecom.loclogger.ui.LocalMainPagerState
import moe.telecom.loclogger.ui.theme.LocalEnableBlur
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBar
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBarBlur
import moe.telecom.loclogger.ui.theme.LocalLayerBackdrop
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
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
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val backdrop = LocalLayerBackdrop.current
    val items = BottomBarDestination.entries

    // Liquid Glass 混合色 - 参考 SukiSU BlurredBar
    val blurColors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.87f))
        )
    )
    // minSdk 33 起渲染着色器全支持，保留运行时判断以兼容个别设备
    val blurSupported = backdrop != null && isRuntimeShaderSupported()

    if (!enableFloatingBottomBar) {
        // 普通导航栏：LayerBackdrop + textureBlur 实时毛玻璃
        val blurActive = enableBlur && blurSupported
        Box(
            modifier = if (blurActive && backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f,
                    colors = blurColors
                )
            } else {
                Modifier
            }
        ) {
            NavigationBar(
                modifier = modifier,
                color = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface,
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
        }
    } else {
        // Liquid Glass 浮动导航栏：LayerBackdrop 捕获页面内容 + textureBlur 实时毛玻璃
        val blurActive = enableFloatingBottomBarBlur && blurSupported
        val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
        FloatingNavigationBar(
            modifier = modifier
                .then(
                    if (blurActive && backdrop != null) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
                            blurRadius = 25f,
                            colors = blurColors
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            color = barColor,
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
