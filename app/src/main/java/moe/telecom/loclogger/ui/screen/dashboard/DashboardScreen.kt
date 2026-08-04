package moe.telecom.loclogger.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import moe.telecom.loclogger.ui.component.map.GpsMapView
import moe.telecom.loclogger.ui.component.map.MapSources
import moe.telecom.loclogger.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mapSource by remember { mutableStateOf(MapSources.OSM.name()) }
    var showMapSourceMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 地图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                GpsMapView(
                    currentLat = uiState.latitude,
                    currentLon = uiState.longitude,
                    mapSourceName = mapSource,
                    followLocation = true,
                    showMyLocation = true,
                    modifier = Modifier.fillMaxSize()
                )

                // 地图源切换按钮
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
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
                                text = { Text(source.name()) },
                                onClick = {
                                    mapSource = source.name()
                                    showMapSourceMenu = false
                                }
                            )
                        }
                    }
                }

                // 定位按钮
                IconButton(
                    onClick = { /* 触发重新定位 */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "定位")
                }

                // 录制状态指示
                if (uiState.isRecording) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.onError)
                            )
                            Text(
                                text = if (uiState.isPaused) "已暂停" else "录制中",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 数据面板
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 经纬度卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LocationRow(
                            label = "纬度",
                            value = uiState.latitude?.let { formatDMS(it, true) } ?: "--"
                        )
                        LocationRow(
                            label = "经度",
                            value = uiState.longitude?.let { formatDMS(it, false) } ?: "--"
                        )
                        LocationRow(
                            label = "时间",
                            value = uiState.time ?: "--"
                        )
                    }
                }

                // 卫星信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "卫星",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.satellitesUsed}/${uiState.satellitesVisible}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GpsStatusIndicator(satellites = uiState.satellitesUsed)
                    }
                }

                // 数据网格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DataCard(
                        label = "高度",
                        value = uiState.altitude?.let { "${it.toInt()} 米" } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        label = "速度",
                        value = uiState.speed?.let { "${String.format("%.1f", it * 3.6)} km/h" } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DataCard(
                        label = "精度",
                        value = uiState.accuracy?.let { "${String.format("%.1f", it)} 米" } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        label = "方向",
                        value = uiState.bearing?.let { bearingToDirection(it) } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 录制统计
                if (uiState.isRecording) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn("点数", uiState.trackPoints.toString())
                            StatColumn("距离", formatDistance(uiState.distance))
                            StatColumn("时长", formatDuration(uiState.duration))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // 开始/停止 FAB
        FloatingActionButton(
            onClick = { viewModel.toggleRecording() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            containerColor = if (uiState.isRecording)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isRecording) "停止" else "开始",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun LocationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
private fun DataCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GpsStatusIndicator(satellites: Int) {
    val color = when {
        satellites >= 10 -> MaterialTheme.colorScheme.primary
        satellites >= 4 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Text(
            text = when {
                satellites >= 10 -> "信号强"
                satellites >= 4 -> "已定位"
                else -> "搜索中"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

private fun formatDMS(decimal: Double, isLatitude: Boolean): String {
    val direction = if (isLatitude) {
        if (decimal >= 0) "北" else "南"
    } else {
        if (decimal >= 0) "东" else "西"
    }
    val abs = Math.abs(decimal)
    val degrees = abs.toInt()
    val minutesFull = (abs - degrees) * 60
    val minutes = minutesFull.toInt()
    val seconds = (minutesFull - minutes) * 60
    return "$degrees°${minutes}' ${String.format("%.5f", seconds)}\" $direction"
}

private fun bearingToDirection(bearing: Float): String {
    val directions = arrayOf(
        "北", "北偏东", "东北偏北", "东北", "东北偏东", "东偏北",
        "东", "东偏南", "东南偏东", "东南", "东南偏南", "南偏东",
        "南", "南偏西", "西南偏南", "西南", "西南偏西", "西偏南",
        "西", "西偏北", "西北偏西", "西北", "西北偏北", "北偏西"
    )
    val index = ((bearing / 15f) + 0.5f).toInt() % 24
    return directions[index]
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f km", meters / 1000)
    else String.format("%.0f m", meters)
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
