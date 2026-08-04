package moe.telecom.loclogger.data.export

import android.content.Context
import android.net.Uri
import moe.telecom.loclogger.data.local.entity.AnnotationEntity
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.local.entity.TrackPointEntity
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 轨迹导出工具
 * 支持格式：GPX (1.0/1.1/2.2)、KML、KMZ、TXT、CSV
 */
object TrackExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // ==================== GPX 导出 ====================

    fun exportGpx(
        out: OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>,
        annotations: List<AnnotationEntity>,
        version: String = "1.1"
    ) {
        val writer = OutputStreamWriter(out, Charsets.UTF_8)
        writer.use { w ->
            val ns = when (version) {
                "1.0" -> "http://www.topografix.com/GPX/1/0"
                "2.2" -> "http://www.topografix.com/GPX/2/2"
                else -> "http://www.topografix.com/GPX/1/1"
            }
            val schemaLoc = when (version) {
                "1.0" -> "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd"
                "2.2" -> "http://www.topografix.com/GPX/2/2 http://www.topografix.com/GPX/2/2/gpx.xsd"
                else -> "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
            }

            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            w.write("<gpx version=\"$version\" creator=\"GPS Logger\" " +
                    "xmlns=\"$ns\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"$schemaLoc\">\n")

            // 元数据
            w.write("  <metadata>\n")
            w.write("    <name>${escapeXml(track.name)}</name>\n")
            w.write("    <time>${isoFormat.format(Date(track.startTime))}</time>\n")
            w.write("  </metadata>\n")

            // 批注作为路点
            annotations.forEach { ann ->
                w.write("  <wpt lat=\"${ann.latitude}\" lon=\"${ann.longitude}\">\n")
                w.write("    <name>${escapeXml(ann.description)}</name>\n")
                w.write("    <time>${isoFormat.format(Date(ann.timestamp))}</time>\n")
                w.write("  </wpt>\n")
            }

            // 轨迹
            w.write("  <trk>\n")
            w.write("    <name>${escapeXml(track.name)}</name>\n")
            w.write("    <trkseg>\n")

            points.forEach { pt ->
                w.write("      <trkpt lat=\"${pt.latitude}\" lon=\"${pt.longitude}\">")
                if (pt.altitude != null) w.write("<ele>${pt.altitude}</ele>")
                w.write("<time>${isoFormat.format(Date(pt.timestamp))}</time>")
                if (pt.speed != null) w.write("<speed>${pt.speed}</speed>")
                if (pt.accuracy != null) {
                    w.write("<extensions><hdop>${pt.accuracy / 5.0}</hdop></extensions>")
                }
                if (pt.satellitesUsed > 0) {
                    w.write("<sat>${pt.satellitesUsed}</sat>")
                }
                w.write("</trkpt>\n")
            }

            w.write("    </trkseg>\n")
            w.write("  </trk>\n")
            w.write("</gpx>\n")
        }
    }

    // ==================== KML 导出 ====================

    fun exportKml(
        out: OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>,
        annotations: List<AnnotationEntity>
    ) {
        val writer = OutputStreamWriter(out, Charsets.UTF_8)
        writer.use { w ->
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            w.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
            w.write("<Document>\n")
            w.write("  <name>${escapeXml(track.name)}</name>\n")

            // 样式
            w.write("  <Style id=\"trackLine\">\n")
            w.write("    <LineStyle><color>ffD4BC00</color><width>4</width></LineStyle>\n")
            w.write("  </Style>\n")
            w.write("  <Style id=\"annotationPoint\">\n")
            w.write("    <IconStyle><scale>1.0</scale></IconStyle>\n")
            w.write("  </Style>\n")

            // 轨迹线
            w.write("  <Placemark>\n")
            w.write("    <name>${escapeXml(track.name)}</name>\n")
            w.write("    <styleUrl>#trackLine</styleUrl>\n")
            w.write("    <LineString>\n")
            w.write("      <tessellate>1</tessellate>\n")
            w.write("      <altitudeMode>clampToGround</altitudeMode>\n")
            w.write("      <coordinates>\n")
            points.forEach { pt ->
                w.write("        ${pt.longitude},${pt.latitude}")
                if (pt.altitude != null) w.write(",${pt.altitude}")
                w.write("\n")
            }
            w.write("      </coordinates>\n")
            w.write("    </LineString>\n")
            w.write("  </Placemark>\n")

            // 批注点
            annotations.forEach { ann ->
                w.write("  <Placemark>\n")
                w.write("    <name>${escapeXml(ann.description)}</name>\n")
                w.write("    <styleUrl>#annotationPoint</styleUrl>\n")
                w.write("    <Point>\n")
                w.write("      <coordinates>${ann.longitude},${ann.latitude}</coordinates>\n")
                w.write("    </Point>\n")
                w.write("  </Placemark>\n")
            }

            w.write("</Document>\n")
            w.write("</kml>\n")
        }
    }

    // ==================== KMZ 导出（KML + ZIP） ====================

    fun exportKmz(
        out: OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>,
        annotations: List<AnnotationEntity>
    ) {
        ZipOutputStream(out.buffered()).use { zip ->
            // KML 文件
            zip.putNextEntry(ZipEntry("doc.kml"))
            exportKml(zip, track, points, annotations)
            zip.closeEntry()
        }
    }

    // ==================== TXT 导出 ====================

    fun exportTxt(
        out: OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>,
        annotations: List<AnnotationEntity>
    ) {
        val writer = OutputStreamWriter(out, Charsets.UTF_8)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        writer.use { w ->
            w.write("GPS Logger 轨迹记录\n")
            w.write("===================\n")
            w.write("名称: ${track.name}\n")
            w.write("开始时间: ${timeFormat.format(Date(track.startTime))}\n")
            track.endTime?.let { w.write("结束时间: ${timeFormat.format(Date(it))}\n") }
            w.write("路点数: ${points.size}\n")
            w.write("批 注: ${annotations.size}\n")
            w.write("距 离: ${"%.2f".format(track.totalDistance)} 米\n")
            w.write("\n")

            w.write("时间,纬度,经度,高度,速度(m/s),精度,卫星\n")
            points.forEach { pt ->
                w.write("${timeFormat.format(Date(pt.timestamp))}," +
                        "${pt.latitude},${pt.longitude}," +
                        "${pt.altitude ?: ""}," +
                        "${pt.speed ?: ""}," +
                        "${pt.accuracy ?: ""}," +
                        "${pt.satellitesUsed}\n")
            }

            if (annotations.isNotEmpty()) {
                w.write("\n批注:\n")
                annotations.forEach { ann ->
                    w.write("${timeFormat.format(Date(ann.timestamp))}: ${ann.description}\n")
                }
            }
        }
    }

    // ==================== CSV 导出 ====================

    fun exportCsv(
        out: OutputStream,
        track: TrackEntity,
        points: List<TrackPointEntity>
    ) {
        val writer = OutputStreamWriter(out, Charsets.UTF_8)
        writer.use { w ->
            w.write("timestamp,latitude,longitude,altitude,speed,accuracy,bearing,satellites\n")
            points.forEach { pt ->
                w.write("${pt.timestamp},${pt.latitude},${pt.longitude}," +
                        "${pt.altitude ?: ""},${pt.speed ?: ""}," +
                        "${pt.accuracy ?: ""},${pt.bearing ?: ""}," +
                        "${pt.satellitesUsed}\n")
            }
        }
    }

    // ==================== 工具方法 ====================

    fun getExportFileName(track: TrackEntity, format: String): String {
        return "${track.name}.$format"
    }

    fun getExportMimeType(format: String): String {
        return when (format) {
            "gpx" -> "application/gpx+xml"
            "kml" -> "application/vnd.google-earth.kml+xml"
            "kmz" -> "application/vnd.google-earth.kmz"
            "csv" -> "text/csv"
            else -> "text/plain"
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
