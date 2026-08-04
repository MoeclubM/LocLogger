package moe.telecom.loclogger.ui.screen.track

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.telecom.loclogger.data.local.entity.AnnotationEntity
import moe.telecom.loclogger.data.repository.TrackingRepository
import moe.telecom.loclogger.ui.component.liquid.GlassCard
import moe.telecom.loclogger.ui.component.map.GpsMapView
import moe.telecom.loclogger.ui.component.map.MapSources
import moe.telecom.loclogger.ui.screen.tracks.TrackItem
import moe.telecom.loclogger.viewmodel.TrackDetail
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TrackDetailUiState(
    val isLoading: Boolean = true,
    val detail: TrackDetail? = null,
    val isSharing: Boolean = false
)

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackDetailUiState())
    val uiState: StateFlow<TrackDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(item: TrackItem) {
        viewModelScope.launch {
            val track = trackingRepository.getTrackById(item.id)
            if (track == null) {
                _uiState.value = TrackDetailUiState(isLoading = false, detail = null)
                return@launch
            }
            val points = trackingRepository.getPointsForTrackSync(item.id)
            val annotations = trackingRepository.getAnnotationsForTrackSync(item.id)
            _uiState.value = TrackDetailUiState(
                isLoading = false,
                detail = TrackDetail(track, points, annotations)
            )
        }
    }

    fun shareTrack(item: TrackItem, format: String, onResult: (Intent) -> Unit) {
        val fmt = format.lowercase()
        if (_uiState.value.isSharing) return
        _uiState.value = _uiState.value.copy(isSharing = true)
        viewModelScope.launch {
            try {
                val track = trackingRepository.getTrackById(item.id)
                    ?: throw IllegalStateException("轨迹不存在")
                val points = trackingRepository.getPointsForTrackSync(item.id)
                val annotations = trackingRepository.getAnnotationsForTrackSync(item.id)

                val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
                val fileName = trackingRepository.getExportFileName(track, fmt)
                val file = File(exportDir, fileName)
                file.outputStream().use { out ->
                    trackingRepository.exportTrack(out, track, points, annotations, fmt)
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mime = trackingRepository.getExportMimeType(fmt)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onResult(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.value = _uiState.value.copy(isSharing = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackItem: TrackItem,
    onBack: () -> Unit,
    viewModel: TrackDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(trackItem.id) {
        viewModel.loadDetail(trackItem)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trackItem.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.detail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("轨迹不存在")
                }
            }

            else -> {
                val detail = uiState.detail!!
                val track = detail.track
                val durationMs = (track.endTime ?: System.currentTimeMillis()) - track.startTime

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    ) {
                        GpsMapView(
                            trackPoints = detail.points.map { it.latitude to it.longitude },
                            annotations = detail.annotations.map {
                                Triple(it.latitude, it.longitude, it.description)
                            },
                            mapSourceName = MapSources.AMAP.name,
                            followLocation = false,
                            showMyLocation = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "统计数据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatItem("距离", formatDistance(track.totalDistance), Modifier.weight(1f))
                                StatItem("持续时间", formatDuration(durationMs), Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatItem("平均速度", String.format("%.1f km/h", track.avgSpeed * 3.6), Modifier.weight(1f))
                                StatItem("最高速度", String.format("%.1f km/h", track.maxSpeed * 3.6), Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatItem("高度差", "${track.altitudeDiff.toInt()} 米", Modifier.weight(1f))
                                StatItem("路点数", track.pointCount.toString(), Modifier.weight(1f))
                            }
                            StatItem("批注数", track.annotationCount.toString(), Modifier.fillMaxWidth())
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "批注列表",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (detail.annotations.isEmpty()) {
                                Text(
                                    text = "暂无批注",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                detail.annotations.forEach { annotation ->
                                    AnnotationItem(annotation)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.shareTrack(trackItem, "gpx") { intent ->
                                context.startActivity(Intent.createChooser(intent, "分享轨迹"))
                            }
                        },
                        enabled = !uiState.isSharing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("分享轨迹")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun AnnotationItem(annotation: AnnotationEntity) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = annotation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = dateFormat.format(Date(annotation.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f 公里", meters / 1000)
    else String.format("%.0f 米", meters)
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}