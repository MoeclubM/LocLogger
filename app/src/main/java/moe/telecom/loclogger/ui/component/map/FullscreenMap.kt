package moe.telecom.loclogger.ui.component.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 全屏地图弹层
 *
 * 覆盖整个窗口（含系统栏与底部导航栏），复用 GpsMapView 的地图源切换、
 * 定位回正与手势能力；关闭即销毁内部地图实例。
 */
@Composable
fun FullscreenMap(
    title: String,
    currentLat: Double? = null,
    currentLon: Double? = null,
    trackPoints: List<Pair<Double, Double>> = emptyList(),
    annotations: List<Triple<Double, Double, String>> = emptyList(),
    mapSourceName: String = MapSources.AMAP.name,
    onMapSourceChange: (String) -> Unit = {},
    showMyLocation: Boolean = true,
    initialFollow: Boolean = true,
    showTrackPoints: Boolean = false,
    onShowTrackPointsChange: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    var mapSource by remember(mapSourceName) { mutableStateOf(mapSourceName) }
    var followLocation by remember { mutableStateOf(initialFollow) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var showMapSourceMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GpsMapView(
                currentLat = currentLat,
                currentLon = currentLon,
                trackPoints = trackPoints,
                annotations = annotations,
                mapSourceName = mapSource,
                followLocation = followLocation,
                showMyLocation = showMyLocation,
                showTrackPoints = showTrackPoints,
                recenterRequest = recenterRequest,
                onUserGesture = { followLocation = false },
                modifier = Modifier.fillMaxSize()
            )

            // 顶部控制条：返回 + 标题 + 地图源切换
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                if (trackPoints.isNotEmpty()) {
                    IconButton(
                        onClick = { onShowTrackPointsChange(!showTrackPoints) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (showTrackPoints) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = if (showTrackPoints) "隐藏记录点" else "显示记录点",
                            tint = if (showTrackPoints) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showMapSourceMenu = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "地图源")
                    }
                    DropdownMenu(
                        expanded = showMapSourceMenu,
                        onDismissRequest = { showMapSourceMenu = false }
                    ) {
                        MapSources.all.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    mapSource = source.name
                                    onMapSourceChange(source.name)
                                    showMapSourceMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 底部定位按钮：回正并强制移动相机
            IconButton(
                onClick = {
                    followLocation = true
                    recenterRequest++
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "定位")
            }
        }
    }
}
