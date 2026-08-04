package moe.telecom.loclogger.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.telecom.loclogger.R
import moe.telecom.loclogger.ui.LocalMainPagerState
import moe.telecom.loclogger.ui.component.liquid.FloatingBottomBar
import moe.telecom.loclogger.ui.component.liquid.FloatingBottomBarItem
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBar
import moe.telecom.loclogger.ui.theme.LocalEnableFloatingBottomBarBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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

// 普通导航栏毛玻璃 - 参考 SukiSU BlurredBar（LayerBackdrop + textureBlur）
@Composable
private fun BlurredBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f))
                    )
                )
            )
        } else {
            Modifier
        }
    ) {
        content()
    }
}

@Composable
fun BottomBarMiuix(
    modifier: Modifier = Modifier,
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val items = BottomBarDestination.entries

    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
            ) {
                items.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = mainState.selectedPage == index,
                        onClick = { mainState.animateToPage(index) },
                        icon = destination.icon,
                        label = stringResource(destination.label)
                    )
                }
            }
        }
    } else {
        // Scaffold 的 bottomBar 槽位默认 start 对齐，需全宽居中
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
        FloatingBottomBar(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            selectedIndex = { mainState.selectedPage },
            onSelected = { mainState.animateToPage(it) },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = enableFloatingBottomBarBlur,
        ) {
            items.forEachIndexed { index, destination ->
                FloatingBottomBarItem(
                    onClick = { mainState.animateToPage(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.label),
                    )
                    Text(
                        text = stringResource(destination.label),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
        }
    }
}
