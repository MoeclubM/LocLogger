package moe.telecom.loclogger.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * EGM96 大地水准面模型（WW15MGH.DAC）
 * 网格 721×1440，0.25° 间隔；格点值为 int16 大端、单位厘米。
 * 用途：海拔 = GPS 椭球高 - 大地水准面差距 N。
 */
object Egm96 {
    private const val NUM_ROWS = 721
    private const val NUM_COLS = 1440
    private const val INTERVAL = 0.25

    @Volatile
    private var grid: ShortArray? = null

    val isLoaded: Boolean get() = grid != null

    /** 从 WW15MGH.DAC 字节流加载网格（int16 大端，厘米） */
    fun load(bytes: ByteArray) {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val count = bytes.size / 2
        val values = ShortArray(count)
        for (i in 0 until count) values[i] = buf.getShort()
        grid = values
    }

    /** 返回经纬度处的大地水准面差距 N（米）；未加载数据返回 null */
    fun geoidHeight(latitude: Double, longitude: Double): Double? {
        val g = grid ?: return null
        var lng = if (longitude < 0) longitude + 360.0 else longitude
        if (lng >= 360.0 - INTERVAL) lng = 360.0 - INTERVAL

        var topRow = ((90.0 - latitude) / INTERVAL).toInt()
        if (topRow < 0) topRow = 0
        if (topRow > NUM_ROWS - 2) topRow = NUM_ROWS - 2
        val bottomRow = topRow + 1

        var leftCol = (lng / INTERVAL).toInt()
        if (leftCol < 0) leftCol = 0
        if (leftCol > NUM_COLS - 1) leftCol = NUM_COLS - 1
        val rightCol = if (leftCol == NUM_COLS - 1) 0 else leftCol + 1

        val latTop = 90.0 - topRow * INTERVAL
        val lonLeft = leftCol * INTERVAL
        val u = (lng - lonLeft) / INTERVAL
        val v = (latTop - latitude) / INTERVAL

        val ul = g[topRow * NUM_COLS + leftCol].toDouble()
        val ur = g[topRow * NUM_COLS + rightCol].toDouble()
        val ll = g[bottomRow * NUM_COLS + leftCol].toDouble()
        val lr = g[bottomRow * NUM_COLS + rightCol].toDouble()

        return ((1 - u) * (1 - v) * ul + u * (1 - v) * ur + (1 - u) * v * ll + u * v * lr) / 100.0
    }
}
