package moe.telecom.loclogger.ui.screen.dashboard

import android.location.GnssStatus
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.telecom.loclogger.data.service.SatelliteInfo
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteDetailSheet(
    used: Int,
    visible: Int,
    satellites: List<SatelliteInfo>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "卫星详情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "用于定位 $used / 可见 $visible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            SkyPlot(
                satellites = satellites,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConstellationLegend()
            }

            if (satellites.isEmpty()) {
                Text(
                    text = "暂无卫星数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val sorted = satellites.sortedWith(
                    compareByDescending<SatelliteInfo> { it.usedInFix }
                        .thenByDescending { it.cn0DbHz }
                )
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sorted, key = { "${it.constellation}-${it.svid}" }) { sat ->
                        SatelliteRow(sat)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyPlot(
    satellites: List<SatelliteInfo>,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy) - 18.dp.toPx()
            val stroke = Stroke(width = 1.dp.toPx())
            drawCircle(color = ringColor, radius = radius, center = Offset(cx, cy), style = stroke)
            drawCircle(color = ringColor, radius = radius * 2f / 3f, center = Offset(cx, cy), style = stroke)
            drawCircle(color = ringColor, radius = radius / 3f, center = Offset(cx, cy), style = stroke)
            drawLine(ringColor, Offset(cx, cy - radius), Offset(cx, cy + radius), 1.dp.toPx())
            drawLine(ringColor, Offset(cx - radius, cy), Offset(cx + radius, cy), 1.dp.toPx())

            satellites.forEach { sat ->
                val r = ((90f - sat.elevation).coerceIn(0f, 90f) / 90f) * radius
                val azimuthRad = Math.toRadians(sat.azimuth.toDouble() - 90.0)
                val x = cx + (r * cos(azimuthRad)).toFloat()
                val y = cy + (r * sin(azimuthRad)).toFloat()
                val color = constellationColor(sat.constellation)
                val dot = if (sat.usedInFix) 6.dp.toPx() else 4.dp.toPx()
                drawCircle(
                    color = color.copy(alpha = if (sat.usedInFix) 1f else 0.45f),
                    radius = dot,
                    center = Offset(x, y)
                )
                if (sat.usedInFix) {
                    drawCircle(
                        color = Color.White,
                        radius = dot,
                        center = Offset(x, y),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            }
        }
        Text(
            "N",
            fontSize = 11.sp,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun ConstellationLegend() {
    val items = listOf(
        "GPS" to constellationColor(GnssStatus.CONSTELLATION_GPS),
        "北斗" to constellationColor(GnssStatus.CONSTELLATION_BEIDOU),
        "GLONASS" to constellationColor(GnssStatus.CONSTELLATION_GLONASS),
        "Galileo" to constellationColor(GnssStatus.CONSTELLATION_GALILEO)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SatelliteRow(sat: SatelliteInfo) {
    val color = constellationColor(sat.constellation)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color.copy(alpha = if (sat.usedInFix) 1f else 0.4f), CircleShape)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${constellationLabel(sat.constellation)} ${sat.svid}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildString {
                    append("仰角 ${sat.elevation.toInt()}°  方位 ${sat.azimuth.toInt()}°")
                    if (sat.hasEphemeris) append("  星历")
                    if (sat.hasAlmanac) append("  历书")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (sat.cn0DbHz > 0f) "${sat.cn0DbHz.toInt()} dBHz" else "--",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (sat.usedInFix) "参与定位" else "未使用",
                fontSize = 11.sp,
                color = if (sat.usedInFix) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun constellationLabel(type: Int): String = when (type) {
    GnssStatus.CONSTELLATION_GPS -> "GPS"
    GnssStatus.CONSTELLATION_SBAS -> "SBAS"
    GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
    GnssStatus.CONSTELLATION_QZSS -> "QZSS"
    GnssStatus.CONSTELLATION_BEIDOU -> "北斗"
    GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
    GnssStatus.CONSTELLATION_IRNSS -> "NavIC"
    else -> "其它"
}

private fun constellationColor(type: Int): Color = when (type) {
    GnssStatus.CONSTELLATION_GPS -> Color(0xFF1E88E5)
    GnssStatus.CONSTELLATION_BEIDOU -> Color(0xFF43A047)
    GnssStatus.CONSTELLATION_GLONASS -> Color(0xFFE53935)
    GnssStatus.CONSTELLATION_GALILEO -> Color(0xFFFB8C00)
    GnssStatus.CONSTELLATION_QZSS -> Color(0xFF8E24AA)
    GnssStatus.CONSTELLATION_SBAS -> Color(0xFF78909C)
    GnssStatus.CONSTELLATION_IRNSS -> Color(0xFF00897B)
    else -> Color(0xFF6D4C41)
}
