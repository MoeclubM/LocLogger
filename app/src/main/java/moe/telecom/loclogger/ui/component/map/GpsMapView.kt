package moe.telecom.loclogger.ui.component.map

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * osmdroid 地图 Composable 组件
 *
 * 支持：
 * - 多地图源切换（OSM/Google/高德）
 * - 实时位置显示
 * - 轨迹绘制
 * - 批注标记
 */
@Composable
fun GpsMapView(
    modifier: Modifier = Modifier,
    currentLat: Double? = null,
    currentLon: Double? = null,
    trackPoints: List<Pair<Double, Double>> = emptyList(),
    annotations: List<Triple<Double, Double, String>> = emptyList(),
    mapSourceName: String = "OpenStreetMap",
    followLocation: Boolean = true,
    showMyLocation: Boolean = true
) {
    val context = LocalContext.current

    val mapView = remember {
        // 初始化 osmdroid 配置（须在创建 MapView 前设置）
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            tileFileSystemCacheMaxBytes = 500L * 1024 * 1024 // 500MB 缓存
        }
        MapView(context).apply {
            setTileSource(MapSources.OSM)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
            setMinZoomLevel(3.0)
            setMaxZoomLevel(20.0)
            isTilesScaledToDpi = true
        }
    }

    // 位置覆盖层
    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }

    // 轨迹线
    val trackPolyline = remember {
        Polyline().apply {
            outlinePaint.color = Color.parseColor("#00BCD4")
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
            infoWindow = null
        }
    }

    LaunchedEffect(showMyLocation) {
        if (showMyLocation) {
            if (mapView.overlays.none { it is MyLocationNewOverlay }) {
                mapView.overlays.add(0, locationOverlay)
            }
            locationOverlay.enableMyLocation()
            if (followLocation) locationOverlay.enableFollowLocation()
        } else {
            locationOverlay.disableMyLocation()
            locationOverlay.disableFollowLocation()
            mapView.overlays.remove(locationOverlay)
        }
    }

    // 地图源切换
    LaunchedEffect(mapSourceName) {
        val source = MapSources.fromName(mapSourceName)
        mapView.setTileSource(source)
    }

    // 跟随当前位置
    LaunchedEffect(currentLat, currentLon, followLocation) {
        if (followLocation && currentLat != null && currentLon != null) {
            val point = GeoPoint(currentLat, currentLon)
            mapView.controller.animateTo(point)
            if (!showMyLocation) {
                mapView.controller.setCenter(point)
            }
        }
    }

    // 绘制轨迹
    LaunchedEffect(trackPoints) {
        if (trackPoints.isNotEmpty()) {
            val geoPoints = trackPoints.map { (lat, lon) -> GeoPoint(lat, lon) }
            trackPolyline.setPoints(geoPoints)
            if (mapView.overlays.none { it === trackPolyline }) {
                mapView.overlays.add(trackPolyline)
            }
            // 自动缩放到轨迹范围
            if (!followLocation && geoPoints.size > 1) {
                mapView.post {
                    var minLat = Double.MAX_VALUE
                    var maxLat = -Double.MAX_VALUE
                    var minLon = Double.MAX_VALUE
                    var maxLon = -Double.MAX_VALUE
                    geoPoints.forEach { p ->
                        if (p.latitude < minLat) minLat = p.latitude
                        if (p.latitude > maxLat) maxLat = p.latitude
                        if (p.longitude < minLon) minLon = p.longitude
                        if (p.longitude > maxLon) maxLon = p.longitude
                    }
                    val center = GeoPoint((minLat + maxLat) / 2, (minLon + maxLon) / 2)
                    mapView.controller.setCenter(center)
                    mapView.zoomToBoundingBox(
                        org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon),
                        true, 100
                    )
                }
            }
        } else {
            mapView.overlays.remove(trackPolyline)
        }
        mapView.invalidate()
    }

    // 绘制批注标记
    LaunchedEffect(annotations) {
        // 移除旧的批注标记
        mapView.overlays.removeAll { it is Marker && it.id?.startsWith("annotation_") == true }

        annotations.forEachIndexed { index, (lat, lon, desc) ->
            val marker = Marker(mapView).apply {
                id = "annotation_$index"
                position = GeoPoint(lat, lon)
                title = desc
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // 可以自定义图标
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    // 绑定生命周期：osmdroid 必须调用 onResume/onPause 才会加载地图瓦片
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationOverlay.disableMyLocation()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { it.invalidate() }
    )
}
