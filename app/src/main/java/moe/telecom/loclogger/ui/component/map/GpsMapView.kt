package moe.telecom.loclogger.ui.component.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * MapLibre 地图 Composable 组件
 *
 * 用 raster 瓦片 style JSON 渲染多地图源（OSM/Google/高德），
 * 轨迹 / 批注 / 定位点用 GeoJSON source + style layer 绘制，
 * 不依赖 osmdroid，避免旧实现瓦片不加载、onDraw 刷屏导致 UI 闪烁。
 */
@Composable
fun GpsMapView(
    modifier: Modifier = Modifier,
    currentLat: Double? = null,
    currentLon: Double? = null,
    trackPoints: List<Pair<Double, Double>> = emptyList(),
    annotations: List<Triple<Double, Double, String>> = emptyList(),
    mapSourceName: String = "高德地图",
    followLocation: Boolean = true,
    showMyLocation: Boolean = true,
    recenterRequest: Int = 0,
    onUserGesture: () -> Unit = {}
) {
    val context = LocalContext.current
    val state = remember { MapViewState() }
    val currentSource by rememberUpdatedState(mapSourceName)

    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    // 初始化地图并加载默认样式
    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            state.map = map
            // 定位前先把相机移到中国范围，避免高德在默认(0,0)境外无数据返回空白瓦片
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(35.0, 105.0), 4.0)
            )
            // 用户开始拖动地图时通知外部关闭跟随，避免相机被定位更新拉回
            map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) = onUserGesture()
                override fun onMove(detector: MoveGestureDetector) = Unit
                override fun onMoveEnd(detector: MoveGestureDetector) = Unit
            })
            applySource(state, currentSource)
        }
        onDispose {}
    }

    // 定位按钮回正请求：下次定位更新时强制移动相机
    LaunchedEffect(recenterRequest) {
        if (recenterRequest > 0) state.forceMove = true
    }

    // 地图源切换
    LaunchedEffect(mapSourceName) {
        applySource(state, mapSourceName)
    }

    // 跟随当前位置
    LaunchedEffect(currentLat, currentLon, followLocation) {
        if (currentLat != null && currentLon != null) {
            state.currentLat = currentLat
            state.currentLon = currentLon
            updateLocation(state, showMyLocation)
            if (followLocation) {
                moveCamera(state, currentLat, currentLon, force = state.style == null || state.forceMove)
                state.forceMove = false
            }
        }
    }

    // 定位点显隐
    LaunchedEffect(showMyLocation) {
        updateLocation(state, showMyLocation)
    }

    // 轨迹绘制
    LaunchedEffect(trackPoints) {
        state.trackPoints = trackPoints
        updateTrack(state)
        if (!followLocation && trackPoints.size > 1) zoomToTrack(state)
    }

    // 批注标记
    LaunchedEffect(annotations) {
        state.annotations = annotations
        updateAnnotations(state)
    }

    // MapLibre 生命周期绑定：onCreate/onStart/onResume/onPause/onStop/onDestroy
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    if (!destroyed) {
                        destroyed = true
                        mapView.onDestroy()
                    }
                }
                else -> {}
            }
        }
        val current = lifecycleOwner.lifecycle.currentState
        if (current.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (current.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}

/** 保存 MapLibre 地图实例与待绘制数据，跨重组/换样式复用 */
private class MapViewState {
    var map: MapLibreMap? = null
    var style: Style? = null
    var loadedSource: String? = null
    var currentLat: Double? = null
    var currentLon: Double? = null
    var forceMove = false
    var trackPoints: List<Pair<Double, Double>> = emptyList()
    var annotations: List<Triple<Double, Double, String>> = emptyList()
}

/** 按地图源名加载 raster 样式；同源不重复加载 */
private fun applySource(state: MapViewState, sourceName: String) {
    val map = state.map ?: return
    val def = MapSources.fromName(sourceName)
    if (state.loadedSource == def.name) return
    state.loadedSource = def.name
    map.setStyle(Style.Builder().fromJson(def.styleJson())) { style ->
        state.style = style
        addOverlayLayers(style)
        updateTrack(state)
        updateAnnotations(state)
        updateLocation(state, show = true)
    }
}

/** 每次换样式后把叠加层（轨迹/批注/定位点）重新挂到 raster 层之上 */
private fun addOverlayLayers(style: Style) {
    if (style.getLayer("track-layer") != null) return

    style.addImage("annotation-pin", createPinBitmap())
    style.addSource(GeoJsonSource("track-source", FeatureCollection.fromFeatures(emptyList())))
    style.addSource(GeoJsonSource("annotation-source", FeatureCollection.fromFeatures(emptyList())))
    style.addSource(GeoJsonSource("location-source", FeatureCollection.fromFeatures(emptyList())))

    style.addLayerBelow(
        LineLayer("track-layer", "track-source").withProperties(
            PropertyFactory.lineColor("#00BCD4"),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        ),
        "tiles"
    )

    style.addLayerBelow(
        SymbolLayer("annotation-layer", "annotation-source").withProperties(
            PropertyFactory.iconImage("annotation-pin"),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.textField(Expression.get("title")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor("#333333"),
            PropertyFactory.textHaloColor("#FFFFFF"),
            PropertyFactory.textHaloWidth(1.5f),
            PropertyFactory.textOffset(arrayOf(0f, 1.6f))
        ),
        "tiles"
    )

    style.addLayerBelow(
        CircleLayer("location-layer", "location-source").withProperties(
            PropertyFactory.circleColor("#1E88E5"),
            PropertyFactory.circleRadius(9f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f)
        ),
        "tiles"
    )
}

private fun updateTrack(state: MapViewState) {
    val style = state.style ?: return
    val source = style.getSourceAs<GeoJsonSource>("track-source") ?: return
    val pts = state.trackPoints
    val features = if (pts.size >= 2) {
        val line = LineString.fromLngLats(pts.map { (lat, lon) -> Point.fromLngLat(lon, lat) })
        listOf(Feature.fromGeometry(line))
    } else {
        emptyList()
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateAnnotations(state: MapViewState) {
    val style = state.style ?: return
    val source = style.getSourceAs<GeoJsonSource>("annotation-source") ?: return
    val features = state.annotations.map { (lat, lon, desc) ->
        Feature.fromGeometry(Point.fromLngLat(lon, lat)).also { it.addStringProperty("title", desc) }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateLocation(state: MapViewState, show: Boolean) {
    val style = state.style ?: return
    val source = style.getSourceAs<GeoJsonSource>("location-source") ?: return
    val lat = state.currentLat
    val lon = state.currentLon
    val features = if (show && lat != null && lon != null) {
        listOf(Feature.fromGeometry(Point.fromLngLat(lon, lat)))
    } else {
        emptyList()
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** 跟随定位：仅在首次定位或离视野中心较远时移动相机，避免频繁动画 */
private fun moveCamera(state: MapViewState, lat: Double, lon: Double, force: Boolean) {
    val map = state.map ?: return
    val target = LatLng(lat, lon)
    val camera = map.cameraPosition
    val distToCenter = camera.target?.distanceTo(target) ?: Double.MAX_VALUE
    val needsMove = force || camera.zoom < 14.0 || distToCenter > 150.0
    if (needsMove) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(target, camera.zoom.coerceAtLeast(16.0)),
            600
        )
    }
}

/** 缩放到整条轨迹范围 */
private fun zoomToTrack(state: MapViewState) {
    val map = state.map ?: return
    val pts = state.trackPoints
    if (pts.size < 2) return
    val bounds = LatLngBounds.Builder().includes(pts.map { (lat, lon) -> LatLng(lat, lon) }).build()
    map.getCameraForLatLngBounds(bounds, intArrayOf(48, 48, 48, 48))?.let { position ->
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600)
    }
}

/** 生成批注用的红色定位钉位图 */
private fun createPinBitmap(): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }
    val head = Path().apply {
        moveTo(size * 0.50f, size * 0.92f)
        lineTo(size * 0.22f, size * 0.40f)
        cubicTo(size * 0.22f, size * 0.18f, size * 0.35f, size * 0.06f, size * 0.50f, size * 0.06f)
        cubicTo(size * 0.65f, size * 0.06f, size * 0.78f, size * 0.18f, size * 0.78f, size * 0.40f)
        lineTo(size * 0.50f, size * 0.92f)
        close()
    }
    canvas.drawPath(head, body)
    canvas.drawCircle(
        size * 0.50f, size * 0.28f, size * 0.10f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    )
    return bitmap
}
