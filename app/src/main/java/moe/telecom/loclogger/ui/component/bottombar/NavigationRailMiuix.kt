package moe.telecom.loclogger.ui.component.bottombar

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.ui.LocalMainPagerState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun NavigationRailMiuix(
    modifier: Modifier = Modifier
) {
    val mainState = LocalMainPagerState.current
    val items = BottomBarDestination.entries

    NavigationRail(
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        items.forEachIndexed { index, destination ->
            NavigationRailItem(
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
                selected = mainState.selectedPage == index,
                onClick = { mainState.animateToPage(index) }
            )
        }
    }
}
