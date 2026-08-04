package moe.telecom.loclogger.ui.component.bottombar

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.ui.LocalMainPagerState
import androidx.compose.material3.MaterialTheme

@Composable
fun NavigationRailMaterial(
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
                onClick = { mainState.animateToPage(index) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
