package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import com.gpsplane.app.data.model.SatelliteInfo
import org.junit.Test

class SatelliteStatsTest {

    // Hard-coded constellation ids (android.location.GnssStatus) — the platform
    // android.jar stub in unit tests returns 0 for every constant so we can't
    // reference GnssStatus.CONSTELLATION_* directly without Robolectric.
    private companion object {
        const val GPS = 1
        const val SBAS = 2
        const val GLONASS = 3
        const val QZSS = 4
        const val BEIDOU = 5
        const val GALILEO = 6
        const val IRNSS = 7
    }

    private fun sat(constellation: Int, used: Boolean, svid: Int = 1) = SatelliteInfo(
        svid = svid,
        constellationType = constellation,
        cn0DbHz = 30f,
        azimuth = 0f,
        elevation = 45f,
        usedInFix = used,
        hasEphemeris = true,
        hasAlmanac = true
    )

    @Test
    fun `empty list yields zero counts`() {
        val counts = SatelliteStats.countUsedInFix(emptyList())
        assertThat(counts).isEqualTo(SatelliteCounts())
    }

    @Test
    fun `unused sats are ignored`() {
        val counts = SatelliteStats.countUsedInFix(
            listOf(sat(GPS, used = false), sat(BEIDOU, used = false))
        )
        assertThat(counts.total).isEqualTo(0)
        assertThat(counts.gps).isEqualTo(0)
        assertThat(counts.beidou).isEqualTo(0)
    }

    @Test
    fun `counts are grouped by constellation`() {
        val counts = SatelliteStats.countUsedInFix(
            listOf(
                sat(GPS, used = true, svid = 1),
                sat(GPS, used = true, svid = 2),
                sat(GLONASS, used = true),
                sat(BEIDOU, used = true),
                sat(BEIDOU, used = true),
                sat(BEIDOU, used = true),
                sat(GALILEO, used = true),
                sat(QZSS, used = true),
                sat(IRNSS, used = true),
                sat(GPS, used = false), // ignored
            )
        )
        assertThat(counts.total).isEqualTo(9)
        assertThat(counts.gps).isEqualTo(2)
        assertThat(counts.glonass).isEqualTo(1)
        assertThat(counts.beidou).isEqualTo(3)
        assertThat(counts.galileo).isEqualTo(1)
        assertThat(counts.qzss).isEqualTo(1)
        assertThat(counts.irnss).isEqualTo(1)
        assertThat(counts.other).isEqualTo(0)
    }

    @Test
    fun `unknown constellation falls into other bucket`() {
        val counts = SatelliteStats.countUsedInFix(
            listOf(sat(SBAS, used = true), sat(99, used = true))
        )
        assertThat(counts.total).isEqualTo(2)
        assertThat(counts.other).isEqualTo(2)
        assertThat(counts.gps).isEqualTo(0)
    }
}
