package com.gpsplane.app.data.track

import com.google.common.truth.Truth.assertThat
import com.gpsplane.app.data.model.GpsData
import org.junit.Test
import java.io.StringWriter

class GpxWriterTest {

    private fun sampleGps(
        lat: Double = 40.1, lon: Double = -74.2,
        alt: Double = 1234.5, speed: Float = 123.4f, bearing: Float = 90.0f,
        accuracy: Float = 4.5f, sats: Int = 10,
        t: Long = 1746873127000L, // 2025-05-10T10:32:07Z
    ) = GpsData(
        latitude = lat, longitude = lon,
        altitudeMeters = alt, speedMps = speed, bearing = bearing,
        accuracyMeters = accuracy, verticalSpeedMps = 0f,
        satelliteCount = sats,
        gpsSatelliteCount = 0, glonassSatelliteCount = 0,
        beidouSatelliteCount = 0, galileoSatelliteCount = 0,
        qzssSatelliteCount = 0, irnssSatelliteCount = 0,
        otherSatelliteCount = 0, satellites = emptyList(),
        ttffSeconds = -1f, timestampMs = t, hasFix = true,
    )

    @Test fun `formatTime is ISO-8601 UTC with trailing Z`() {
        assertThat(GpxWriter.formatTime(1746873127000L)).isEqualTo("2025-05-10T10:32:07Z")
    }

    @Test fun `formatCoord emits 6 decimals with a dot regardless of locale`() {
        assertThat(GpxWriter.formatCoord(40.1234567)).isEqualTo("40.123457")
        assertThat(GpxWriter.formatCoord(-74.0)).isEqualTo("-74.000000")
    }

    @Test fun `formatAlt and formatSpeed use 1 and 2 decimals`() {
        assertThat(GpxWriter.formatAlt(1234.555)).isEqualTo("1234.6")
        assertThat(GpxWriter.formatSpeed(123.456f)).isEqualTo("123.46")
    }

    @Test fun `header opens GPX 1 1 with correct namespaces`() {
        val w = StringWriter()
        GpxWriter.writeHeader(w, "Stratos/x", "track", 0L)
        val out = w.toString()
        assertThat(out).contains("""xmlns="http://www.topografix.com/GPX/1/1"""")
        assertThat(out).contains("""xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v2"""")
        assertThat(out).contains("""version="1.1"""")
        assertThat(out).contains("<trk>")
        assertThat(out).contains("<trkseg>")
    }

    @Test fun `footer closes in the right order`() {
        val w = StringWriter()
        GpxWriter.writeFooter(w)
        val lines = w.toString().lines().filter { it.isNotEmpty() }
        assertThat(lines).containsExactly("    </trkseg>", "  </trk>", "</gpx>").inOrder()
    }

    @Test fun `writePoint emits all expected fields`() {
        val w = StringWriter()
        GpxWriter.writePoint(w, sampleGps())
        val out = w.toString()
        assertThat(out).contains("""<trkpt lat="40.100000" lon="-74.200000">""")
        assertThat(out).contains("<ele>1234.5</ele>")
        assertThat(out).contains("<time>2025-05-10T10:32:07Z</time>")
        assertThat(out).contains("<sat>10</sat>")
        assertThat(out).contains("<hdop>4.5</hdop>")
        assertThat(out).contains("<gpxtpx:speed>123.40</gpxtpx:speed>")
        assertThat(out).contains("<gpxtpx:course>90.0</gpxtpx:course>")
    }

    @Test fun `special chars in creator are escaped`() {
        val w = StringWriter()
        GpxWriter.writeHeader(w, "S&T <v1>", "t", 0L)
        val out = w.toString()
        assertThat(out).contains("S&amp;T &lt;v1&gt;")
        assertThat(out).doesNotContain("S&T <v1>")
    }

    @Test fun `full document is parseable as valid XML`() {
        val w = StringWriter()
        GpxWriter.writeHeader(w, "Stratos/test", "track-1", 1746873127000L)
        GpxWriter.writePoint(w, sampleGps())
        GpxWriter.writePoint(w, sampleGps(lat = 40.2, t = 1746873128000L))
        GpxWriter.writeFooter(w)
        val xml = w.toString()

        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xml.byteInputStream())
        val trkpts = doc.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", "trkpt")
        assertThat(trkpts.length).isEqualTo(2)
    }
}
