package com.gpsplane.app.data.track

import com.google.common.truth.Truth.assertThat
import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.model.GpsData
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.StringWriter
import java.io.Writer

class TrackRecorderTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun sampleGps(t: Long = 1746873127000L, hasFix: Boolean = true) = GpsData(
        latitude = 40.1, longitude = -74.2, altitudeMeters = 1000.0,
        speedMps = 100f, bearing = 90f, accuracyMeters = 3f,
        verticalSpeedMps = 0f,
        satelliteCount = 10,
        gpsSatelliteCount = 0, glonassSatelliteCount = 0,
        beidouSatelliteCount = 0, galileoSatelliteCount = 0,
        qzssSatelliteCount = 0, irnssSatelliteCount = 0,
        otherSatelliteCount = 0, satellites = emptyList(),
        ttffSeconds = -1f, timestampMs = t, hasFix = hasFix,
    )

    private fun recorder(
        root: File = tmp.newFolder(),
        clock: () -> Long = { 1746873127000L },
        captured: MutableList<StringWriter> = mutableListOf(),
    ) = TrackRecorder(
        root = root,
        creator = "Stratos/test",
        writerFactory = { StringWriter().also { captured.add(it) } as Writer },
        clock = clock,
    )

    @Test fun `no recording while GROUND`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.GROUND)
        assertThat(captured).isEmpty()
        assertThat(r.state().recording).isFalse()
    }

    @Test fun `AIRBORNE transition opens a file and writes a point`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        assertThat(captured).hasSize(1)
        assertThat(captured[0].toString()).contains("<trk>")
        assertThat(captured[0].toString()).contains("<trkpt")
        assertThat(r.state().recording).isTrue()
        assertThat(r.state().pointsWritten).isEqualTo(1)
    }

    @Test fun `subsequent AIRBORNE samples append points`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(t = 1000L), FlightPhase.AIRBORNE)
        r.onGpsSample(sampleGps(t = 2000L), FlightPhase.AIRBORNE)
        r.onGpsSample(sampleGps(t = 3000L), FlightPhase.AIRBORNE)
        assertThat(captured).hasSize(1)
        assertThat(r.state().pointsWritten).isEqualTo(3)
        val count = captured[0].toString().split("<trkpt").size - 1
        assertThat(count).isEqualTo(3)
    }

    @Test fun `AIRBORNE to GROUND closes the file`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        r.onGpsSample(sampleGps(), FlightPhase.GROUND)
        assertThat(r.state().recording).isFalse()
        assertThat(captured[0].toString()).contains("</gpx>")
    }

    @Test fun `second AIRBORNE cycle opens a new file`() {
        val captured = mutableListOf<StringWriter>()
        var now = 1_000L
        val r = recorder(captured = captured, clock = { now })
        r.onGpsSample(sampleGps(t = now), FlightPhase.AIRBORNE)
        r.onGpsSample(sampleGps(t = now), FlightPhase.GROUND)
        now = 2_000L
        r.onGpsSample(sampleGps(t = now), FlightPhase.AIRBORNE)
        assertThat(captured).hasSize(2)
    }

    @Test fun `disabled recorder drops AIRBORNE samples`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.enabled = false
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        assertThat(captured).isEmpty()
        assertThat(r.state().recording).isFalse()
    }

    @Test fun `disabling mid-flight closes the current file`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        r.enabled = false
        assertThat(captured[0].toString()).contains("</gpx>")
        assertThat(r.state().recording).isFalse()
    }

    @Test fun `re-enabling mid-flight starts a new file at next AIRBORNE sample`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        r.enabled = false
        r.enabled = true
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        assertThat(captured).hasSize(2)
    }

    @Test fun `no-fix samples are skipped even when AIRBORNE`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(hasFix = false), FlightPhase.AIRBORNE)
        assertThat(captured).isEmpty()
    }

    @Test fun `periodic flush fires every N points`() {
        var flushCount = 0
        val captured = java.io.StringWriter()
        val probingWriter = object : java.io.Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) = captured.write(cbuf, off, len)
            override fun flush() { flushCount++; captured.flush() }
            override fun close() = captured.close()
        }
        val r = TrackRecorder(
            root = tmp.newFolder(),
            creator = "Stratos/test",
            writerFactory = { probingWriter },
            clock = { 1746873127000L },
            flushIntervalPoints = 3,
        )
        repeat(10) { r.onGpsSample(sampleGps(t = 1000L + it), FlightPhase.AIRBORNE) }
        // Expect flushes after points 3, 6, 9 — the 10th sample hasn't hit
        // a multiple of 3 yet, so flush count is 3.
        assertThat(flushCount).isEqualTo(3)
    }

    @Test fun `GROUND sample after AIRBORNE closes file even without fix`() {
        val captured = mutableListOf<StringWriter>()
        val r = recorder(captured = captured)
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        r.onGpsSample(sampleGps(hasFix = false), FlightPhase.GROUND)
        assertThat(r.state().recording).isFalse()
        assertThat(captured[0].toString()).contains("</gpx>")
    }

    @Test fun `filename uses UTC timestamp formatted as YYYYMMDD-HHMMSS`() {
        val root = tmp.newFolder()
        val r = TrackRecorder(
            root = root,
            creator = "Stratos/test",
            clock = { 1746873127000L }, // 2025-05-10T10:32:07Z
        )
        r.onGpsSample(sampleGps(), FlightPhase.AIRBORNE)
        r.close()
        val files = root.listFiles().orEmpty().map { it.name }
        assertThat(files).containsExactly("20250510-103207.gpx")
    }
}
