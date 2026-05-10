package com.gpsplane.app.data.track

import com.gpsplane.app.data.model.GpsData
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Streaming GPX 1.1 writer. Call [writeHeader] once, [writePoint] per GPS
 * sample, then [writeFooter]. Pure: takes a [Writer], no filesystem or
 * time-of-day reads. Tests pass a [StringWriter].
 *
 * Formatting decisions:
 *  - Lat/lon emit 6 decimal places (~11 cm, well below GPS accuracy).
 *  - Altitude emits 1 decimal place (matches the ~1 m GPS z resolution).
 *  - Time is ISO-8601 UTC with a trailing `Z`, per the GPX 1.1 schema.
 *  - Speed / course / sats ride inside a `<extensions>` block using the
 *    Garmin TrackPointExtension v2 namespace, which most readers (Garmin
 *    Basecamp, MyTracks, gpxpy) accept; unknown tags are ignored.
 */
object GpxWriter {

    private const val GPX_NS = "http://www.topografix.com/GPX/1/1"
    private const val GPX_TPX_NS = "http://www.garmin.com/xmlschemas/TrackPointExtension/v2"

    /** Writes the GPX preamble up to the opening `<trkseg>`. */
    fun writeHeader(writer: Writer, creator: String, trackName: String, startedMs: Long) {
        writer.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        writer.append("""<gpx version="1.1" creator="""")
            .append(escape(creator))
            .append("""" xmlns="""")
            .append(GPX_NS)
            .append("""" xmlns:gpxtpx="""")
            .append(GPX_TPX_NS)
            .append("""">""").append('\n')
        writer.append("  <metadata>").append('\n')
        writer.append("    <name>").append(escape(trackName)).append("</name>").append('\n')
        writer.append("    <time>").append(formatTime(startedMs)).append("</time>").append('\n')
        writer.append("  </metadata>").append('\n')
        writer.append("  <trk>").append('\n')
        writer.append("    <name>").append(escape(trackName)).append("</name>").append('\n')
        writer.append("    <trkseg>").append('\n')
    }

    /** Writes a single `<trkpt>` derived from [gps]. */
    fun writePoint(writer: Writer, gps: GpsData) {
        writer.append("      <trkpt lat=\"")
            .append(formatCoord(gps.latitude))
            .append("\" lon=\"")
            .append(formatCoord(gps.longitude))
            .append("\">").append('\n')
        writer.append("        <ele>").append(formatAlt(gps.altitudeMeters)).append("</ele>").append('\n')
        writer.append("        <time>").append(formatTime(gps.timestampMs)).append("</time>").append('\n')
        writer.append("        <sat>").append(gps.satelliteCount.toString()).append("</sat>").append('\n')
        writer.append("        <hdop>").append(formatAccuracy(gps.accuracyMeters)).append("</hdop>").append('\n')
        writer.append("        <extensions>").append('\n')
        writer.append("          <gpxtpx:TrackPointExtension>").append('\n')
        writer.append("            <gpxtpx:speed>").append(formatSpeed(gps.speedMps)).append("</gpxtpx:speed>").append('\n')
        writer.append("            <gpxtpx:course>").append(formatBearing(gps.bearing)).append("</gpxtpx:course>").append('\n')
        writer.append("          </gpxtpx:TrackPointExtension>").append('\n')
        writer.append("        </extensions>").append('\n')
        writer.append("      </trkpt>").append('\n')
    }

    /** Closes `<trkseg>`, `<trk>`, and `<gpx>`. */
    fun writeFooter(writer: Writer) {
        writer.append("    </trkseg>").append('\n')
        writer.append("  </trk>").append('\n')
        writer.append("</gpx>").append('\n')
    }

    // ── Formatting ──────────────────────────────────────────────────────

    internal fun formatTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    internal fun formatCoord(v: Double): String = "%.6f".format(Locale.US, v)
    internal fun formatAlt(v: Double): String = "%.1f".format(Locale.US, v)
    internal fun formatSpeed(v: Float): String = "%.2f".format(Locale.US, v)
    internal fun formatBearing(v: Float): String = "%.1f".format(Locale.US, v)
    internal fun formatAccuracy(v: Float): String = "%.1f".format(Locale.US, v)

    private fun escape(s: String): String = buildString(s.length + 8) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
