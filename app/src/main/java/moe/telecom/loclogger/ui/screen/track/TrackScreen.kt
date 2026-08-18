package moe.telecom.loclogger.ui.screen.track

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.telecom.loclogger.ui.LocalBottomBarInset
import moe.telecom.loclogger.ui.component.liquid.GlassCard
import moe.telecom.loclogger.ui.component.map.FullscreenMap
import moe.telecom.loclogger.ui.component.map.GpsMapView
import moe.telecom.loclogger.ui.screen.tracks.ActivityType
import moe.telecom.loclogger.viewmodel.SettingsViewModel
import moe.telecom.loclogger.viewmodel.TrackViewModel

@Composable
fun TrackScreen(
    viewModel: TrackViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val mapSource = settings.mapSource
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var annotationText by remember { mutableStateOf("") }
    val trackPoints by viewModel.trackPoints.collectAsState()
    var showFullscreen by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var saveType by remember { mutableStateOf(ActivityType.WALK) }
    var followLocation by remember { mutableStateOf(true) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var showTrackPoints by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "当前轨迹",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 实时地图卡片：当前轨迹线 + 实时定位点
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            GpsMapView(
                currentLat = uiState.latitude,
                currentLon = uiState.longitude,
                trackPoints = trackPoints,
                mapSourceName = mapSource,
                followLocation = followLocation,
                showMyLocation = true,
                showTrackPoints = showTrackPoints,
                recenterRequest = recenterRequest,
                onUserGesture = { followLocation = false },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { showFullscreen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "全屏")
            }
            IconButton(
                onClick = { showTrackPoints = !showTrackPoints },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
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
            IconButton(
                onClick = {
                    followLocation = true
                    recenterRequest++
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "定位")
            }
        }

        if (uiState.isRecording) {
            // 轨迹名称
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "轨迹名称",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.trackName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    if (uiState.isPaused) {
                        Text(
                            text = "已暂停",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 停止记录（置顶显眼）
            Button(
                onClick = {
                    saveName = uiState.trackName
                    saveType = ActivityType.WALK
                    showSaveDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "停止记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 统计数据
            Text(
                text = "统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("路点数", uiState.pointCount.toString(), Modifier.weight(1f))
                StatCard("批注", uiState.annotationCount.toString(), Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("持续时间", formatDuration(uiState.duration), Modifier.weight(1f))
                StatCard("距离", formatDistance(uiState.distance), Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("最高速度", "${String.format("%.1f", uiState.maxSpeed * 3.6)} km/h", Modifier.weight(1f))
                StatCard("平均速度", "${String.format("%.1f", uiState.avgSpeed * 3.6)} km/h", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    "海拔",
                    uiState.altitude?.let { String.format("%.1f 米", it) } ?: "--",
                    Modifier.weight(1f)
                )
                StatCard(
                    "气压",
                    uiState.pressureHpa?.let { String.format("%.1f hPa", it) } ?: "--",
                    Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("高度差", "${uiState.altitudeDiff.toInt()} 米", Modifier.weight(1f))
                StatCard("总体方向", uiState.overallDirection ?: "--", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    "最高海拔",
                    uiState.maxAltitude?.let { String.format("%.1f 米", it) } ?: "--",
                    Modifier.weight(1f)
                )
                StatCard(
                    "最低海拔",
                    uiState.minAltitude?.let { String.format("%.1f 米", it) } ?: "--",
                    Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 次要操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { viewModel.toggleLock() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (uiState.isLocked)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "锁定")
                }

                FilledIconButton(
                    onClick = {
                        if (uiState.isPaused) viewModel.resume() else viewModel.pause()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Icon(
                        if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (uiState.isPaused) "继续" else "暂停"
                    )
                }

                FilledIconButton(
                    onClick = { showAnnotationDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "批注")
                }

            }
        } else {
            // 未录制状态
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "暂无进行中的轨迹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "点击下方按钮开始记录新轨迹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "开始记录",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomBarInset.current + 16.dp))
    }

    if (showAnnotationDialog) {
        AlertDialog(
            onDismissRequest = { showAnnotationDialog = false },
            title = { Text("添加批注") },
            text = {
                OutlinedTextField(
                    value = annotationText,
                    onValueChange = { annotationText = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (annotationText.isNotBlank()) {
                        viewModel.addAnnotation(annotationText.trim())
                    }
                    annotationText = ""
                    showAnnotationDialog = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = {
                    annotationText = ""
                    showAnnotationDialog = false
                }) { Text("取消") }
            }
        )
    }

    if (showFullscreen) {
        FullscreenMap(
            title = "录制地图",
            currentLat = uiState.latitude,
            currentLon = uiState.longitude,
            trackPoints = trackPoints,
            mapSourceName = mapSource,
            onMapSourceChange = { settingsViewModel.updateMapSource(it) },
            showTrackPoints = showTrackPoints,
            onShowTrackPointsChange = { showTrackPoints = it },
            onClose = { showFullscreen = false }
        )
    }

    if (showSaveDialog) {
        SaveTrackDialog(
            name = saveName,
            onNameChange = { saveName = it },
            selectedType = saveType,
            onTypeChange = { saveType = it },
            onConfirm = {
                viewModel.stop(saveName.trim().ifBlank { uiState.trackName }, saveType.ordinal)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveTrackDialog(
    name: String,
    onNameChange: (String) -> Unit,
    selectedType: ActivityType,
    onTypeChange: (ActivityType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "记录类型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActivityType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.label) },
                            leadingIcon = {
                                Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f 公里", meters / 1000)
    else String.format("%.0f 米", meters)
}
