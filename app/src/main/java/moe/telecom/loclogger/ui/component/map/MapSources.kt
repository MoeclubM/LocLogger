package moe.telecom.loclogger.ui.component.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * 地图源定义 - 支持 OSM/Google/高德/天地图等
 */
object MapSources {

    // OpenStreetMap 标准
    val OSM = XYTileSource(
        "OpenStreetMap",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        ),
        "© OpenStreetMap contributors"
    )

    // OpenStreetMap 中文（高德风格）
    val OSM_CN = XYTileSource(
        "OSM中文",
        0, 19, 256, ".png",
        arrayOf("https://tile.openstreetmap.de/"),
        "© OpenStreetMap"
    )

    // Google 地图
    val GOOGLE_MAP = XYTileSource(
        "Google地图",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=m&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt1.google.com/vt/lyrs=m&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt2.google.com/vt/lyrs=m&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt3.google.com/vt/lyrs=m&hl=zh-CN&x=%d&y=%d&z=%d"
        ),
        "© Google"
    )

    // Google 卫星
    val GOOGLE_SATELLITE = XYTileSource(
        "Google卫星",
        0, 20, 256, ".jpg",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=s&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt1.google.com/vt/lyrs=s&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt2.google.com/vt/lyrs=s&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt3.google.com/vt/lyrs=s&hl=zh-CN&x=%d&y=%d&z=%d"
        ),
        "© Google"
    )

    // Google 地形
    val GOOGLE_TERRAIN = XYTileSource(
        "Google地形",
        0, 18, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=p&hl=zh-CN&x=%d&y=%d&z=%d",
            "https://mt1.google.com/vt/lyrs=p&hl=zh-CN&x=%d&y=%d&z=%d"
        ),
        "© Google"
    )

    // 高德地图
    val AMAP = object : OnlineTileSourceBase(
        "高德地图",
        0, 18, 256, ".png",
        arrayOf(
            "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
            "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
            "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
            "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8"
        ),
        "© 高德地图"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return getBaseUrl() +
                "&x=" + MapTileIndex.getX(pMapTileIndex) +
                "&y=" + MapTileIndex.getY(pMapTileIndex) +
                "&z=" + MapTileIndex.getZoom(pMapTileIndex)
        }
    }

    // 高德卫星
    val AMAP_SATELLITE = object : OnlineTileSourceBase(
        "高德卫星",
        0, 18, 256, ".jpg",
        arrayOf(
            "https://webst01.is.autonavi.com/appmaptile?style=6",
            "https://webst02.is.autonavi.com/appmaptile?style=6",
            "https://webst03.is.autonavi.com/appmaptile?style=6",
            "https://webst04.is.autonavi.com/appmaptile?style=6"
        ),
        "© 高德地图"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return getBaseUrl() +
                "&x=" + MapTileIndex.getX(pMapTileIndex) +
                "&y=" + MapTileIndex.getY(pMapTileIndex) +
                "&z=" + MapTileIndex.getZoom(pMapTileIndex)
        }
    }

    // 天地图矢量
    val TIANDITU_VECTOR = XYTileSource(
        "天地图",
        0, 18, 256, ".png",
        arrayOf(
            "https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX=%d&TILEROW=%d&TILECOL=%d&tk=YOUR_TOKEN"
        ),
        "© 天地图"
    )

    val all = listOf(
        OSM, OSM_CN, GOOGLE_MAP, GOOGLE_SATELLITE, GOOGLE_TERRAIN,
        AMAP, AMAP_SATELLITE
    )

    fun fromName(name: String) = all.firstOrNull { it.name() == name } ?: OSM
}
