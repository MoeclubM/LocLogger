package moe.telecom.loclogger.ui.component.map

/** 地图源坐标系：高德/Google中国瓦片用 GCJ02，OSM/Google全球用 WGS84 */
enum class CoordinateSystem { WGS84, GCJ02 }

/**
 * 地图源定义 - MapLibre raster 瓦片源
 *
 * MapLibre 用 style JSON 渲染地图，这里把每个地图源做成一个
 * raster source 的 style JSON（瓦片 URL 模板支持 {z}/{x}/{y} 占位符），
 * 切换地图源 = 重新加载对应的 style JSON。
 */
data class MapSourceDef(
    val name: String,
    val tileUrls: List<String>,
    val maxZoom: Int = 19,
    val tileSize: Int = 256,
    val attribution: String = "",
    val coordinateSystem: CoordinateSystem = CoordinateSystem.WGS84
) {
    /** 生成 MapLibre raster style JSON；带初始视野，避免 setStyle 时闪到 (0,0) */
    fun styleJson(): String {
        val tiles = tileUrls.joinToString(",") { "\"$it\"" }
        return """{"version":8,"center":[105,35],"zoom":4,"sources":{"tiles":{"type":"raster","tiles":[$tiles],"tileSize":$tileSize,"maxzoom":$maxZoom,"attribution":"$attribution"}},"layers":[{"id":"tiles","type":"raster","source":"tiles","paint":{"raster-resampling":"linear"}}]}"""
    }
}

/**
 * 地图源列表 - 支持 OSM/Google/高德（卫星/地形）等
 */
object MapSources {

    // OpenStreetMap 标准（官方 Carto 瓦片，中文标签正常且无拼音，合并原 OSM/OSM中文）
    val OSM = MapSourceDef(
        name = "OpenStreetMap",
        tileUrls = listOf(
            "https://a.tile.openstreetmap.org/{z}/{x}/{y}.png",
            "https://b.tile.openstreetmap.org/{z}/{x}/{y}.png",
            "https://c.tile.openstreetmap.org/{z}/{x}/{y}.png"
        ),
        maxZoom = 19,
        attribution = "© OpenStreetMap contributors"
    )

    // Google 地图
    val GOOGLE_MAP = MapSourceDef(
        name = "Google地图",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=m&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=m&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt2.google.com/vt/lyrs=m&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt3.google.com/vt/lyrs=m&hl=zh-CN&scale=2&x={x}&y={y}&z={z}"
        ),
        maxZoom = 20,
        tileSize = 512,
        attribution = "© Google",
        coordinateSystem = CoordinateSystem.GCJ02
    )

    // Google 卫星（影像本身为 WGS84，叠加 GPS 轨迹无需偏移）
    val GOOGLE_SATELLITE = MapSourceDef(
        name = "Google卫星",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=s&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=s&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt2.google.com/vt/lyrs=s&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt3.google.com/vt/lyrs=s&hl=zh-CN&scale=2&x={x}&y={y}&z={z}"
        ),
        maxZoom = 20,
        tileSize = 512,
        attribution = "© Google",
        coordinateSystem = CoordinateSystem.WGS84
    )

    // Google 地形
    val GOOGLE_TERRAIN = MapSourceDef(
        name = "Google地形",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=p&hl=zh-CN&scale=2&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=p&hl=zh-CN&scale=2&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        tileSize = 512,
        attribution = "© Google",
        coordinateSystem = CoordinateSystem.GCJ02
    )

    // 高德地图（scale=2 为 512 视网膜瓦片，放大后更清晰）
    val AMAP = MapSourceDef(
        name = "高德地图",
        tileUrls = listOf(
            "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
            "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
            "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
            "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        tileSize = 512,
        attribution = "© 高德地图",
        coordinateSystem = CoordinateSystem.GCJ02
    )

    // 高德卫星
    val AMAP_SATELLITE = MapSourceDef(
        name = "高德卫星",
        tileUrls = listOf(
            "https://wprd01.is.autonavi.com/appmaptile?style=6&size=1&scl=2&x={x}&y={y}&z={z}",
            "https://wprd02.is.autonavi.com/appmaptile?style=6&size=1&scl=2&x={x}&y={y}&z={z}",
            "https://wprd03.is.autonavi.com/appmaptile?style=6&size=1&scl=2&x={x}&y={y}&z={z}",
            "https://wprd04.is.autonavi.com/appmaptile?style=6&size=1&scl=2&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        tileSize = 512,
        attribution = "© 高德地图",
        coordinateSystem = CoordinateSystem.GCJ02
    )

    val all = listOf(
        OSM, GOOGLE_MAP, GOOGLE_SATELLITE, GOOGLE_TERRAIN,
        AMAP, AMAP_SATELLITE
    )

    fun fromName(name: String) = all.firstOrNull { it.name == name } ?: AMAP

    val names: List<String> get() = all.map { it.name }
}
