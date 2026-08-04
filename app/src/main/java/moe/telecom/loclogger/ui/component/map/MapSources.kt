package moe.telecom.loclogger.ui.component.map

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
    val attribution: String = ""
) {
    /** 生成 MapLibre raster style JSON */
    fun styleJson(): String {
        val tiles = tileUrls.joinToString(",") { "\"$it\"" }
        return """{"version":8,"sources":{"tiles":{"type":"raster","tiles":[$tiles],"tileSize":$tileSize,"maxzoom":$maxZoom,"attribution":"$attribution"}},"layers":[{"id":"tiles","type":"raster","source":"tiles"}]}"""
    }
}

/**
 * 地图源列表 - 支持 OSM/Google/高德（卫星/地形）等
 */
object MapSources {

    // OpenStreetMap 标准
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

    // OpenStreetMap 中文（高德风格）
    val OSM_CN = MapSourceDef(
        name = "OSM中文",
        tileUrls = listOf("https://tile.openstreetmap.de/{z}/{x}/{y}.png"),
        maxZoom = 19,
        attribution = "© OpenStreetMap"
    )

    // Google 地图
    val GOOGLE_MAP = MapSourceDef(
        name = "Google地图",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=m&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=m&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt2.google.com/vt/lyrs=m&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt3.google.com/vt/lyrs=m&hl=zh-CN&x={x}&y={y}&z={z}"
        ),
        maxZoom = 20,
        attribution = "© Google"
    )

    // Google 卫星
    val GOOGLE_SATELLITE = MapSourceDef(
        name = "Google卫星",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=s&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=s&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt2.google.com/vt/lyrs=s&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt3.google.com/vt/lyrs=s&hl=zh-CN&x={x}&y={y}&z={z}"
        ),
        maxZoom = 20,
        attribution = "© Google"
    )

    // Google 地形
    val GOOGLE_TERRAIN = MapSourceDef(
        name = "Google地形",
        tileUrls = listOf(
            "https://mt0.google.com/vt/lyrs=p&hl=zh-CN&x={x}&y={y}&z={z}",
            "https://mt1.google.com/vt/lyrs=p&hl=zh-CN&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        attribution = "© Google"
    )

    // 高德地图
    val AMAP = MapSourceDef(
        name = "高德地图",
        tileUrls = listOf(
            "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
            "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
            "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
            "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        attribution = "© 高德地图"
    )

    // 高德卫星
    val AMAP_SATELLITE = MapSourceDef(
        name = "高德卫星",
        tileUrls = listOf(
            "https://webst01.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
            "https://webst02.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
            "https://webst03.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
            "https://webst04.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}"
        ),
        maxZoom = 18,
        attribution = "© 高德地图"
    )

    val all = listOf(
        OSM, OSM_CN, GOOGLE_MAP, GOOGLE_SATELLITE, GOOGLE_TERRAIN,
        AMAP, AMAP_SATELLITE
    )

    fun fromName(name: String) = all.firstOrNull { it.name == name } ?: OSM
}
