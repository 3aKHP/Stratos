package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.osmdroid.util.GeoPoint

class TilePreloaderTest {

    @Test
    fun `great circle distance — same point is zero`() {
        val p = GeoPoint(45.0, 120.0)
        assertThat(TilePreloader.greatCircleDistance(p, p)).isEqualTo(0.0)
    }

    @Test
    fun `great circle distance — Beijing to Shanghai`() {
        val beijing = GeoPoint(39.9, 116.4)
        val shanghai = GeoPoint(31.2, 121.5)
        val dist = TilePreloader.greatCircleDistance(beijing, shanghai)
        // ~1060 km
        assertThat(dist).isWithin(50.0).of(1060.0)
    }

    @Test
    fun `great circle distance is symmetric`() {
        val a = GeoPoint(30.0, 100.0)
        val b = GeoPoint(40.0, 110.0)
        assertThat(TilePreloader.greatCircleDistance(a, b))
            .isWithin(1e-6).of(TilePreloader.greatCircleDistance(b, a))
    }

    @Test
    fun `interpolate — midpoint of two points`() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(0.0, 90.0)
        val mid = TilePreloader.interpolate(a, b, 0.5)
        // Midpoint along the equator should be at ~45°E
        assertThat(mid.latitude).isWithin(1e-6).of(0.0)
        assertThat(mid.longitude).isWithin(1e-6).of(45.0)
    }

    @Test
    fun `interpolate — f=0 returns point a`() {
        val a = GeoPoint(39.9, 116.4)
        val b = GeoPoint(31.2, 121.5)
        val result = TilePreloader.interpolate(a, b, 0.0)
        assertThat(result.latitude).isWithin(1e-8).of(a.latitude)
        assertThat(result.longitude).isWithin(1e-8).of(a.longitude)
    }

    @Test
    fun `interpolate — f=1 returns point b`() {
        val a = GeoPoint(39.9, 116.4)
        val b = GeoPoint(31.2, 121.5)
        val result = TilePreloader.interpolate(a, b, 1.0)
        assertThat(result.latitude).isWithin(1e-8).of(b.latitude)
        assertThat(result.longitude).isWithin(1e-8).of(b.longitude)
    }

    @Test
    fun `interpolate — identical points returns the point`() {
        val p = GeoPoint(45.75, 126.63)
        val result = TilePreloader.interpolate(p, p, 0.5)
        assertThat(result.latitude).isWithin(1e-8).of(p.latitude)
        assertThat(result.longitude).isWithin(1e-8).of(p.longitude)
    }

    @Test
    fun `bresenham line — horizontal`() {
        val points = TilePreloader.bresenhamLine(0, 0, 3, 0)
        assertThat(points).hasSize(4)
        assertThat(points.first()).isEqualTo(Pair(0, 0))
        assertThat(points.last()).isEqualTo(Pair(3, 0))
    }

    @Test
    fun `bresenham line — vertical`() {
        val points = TilePreloader.bresenhamLine(0, 0, 0, 3)
        assertThat(points).hasSize(4)
        assertThat(points.first()).isEqualTo(Pair(0, 0))
        assertThat(points.last()).isEqualTo(Pair(0, 3))
    }

    @Test
    fun `bresenham line — single point`() {
        val points = TilePreloader.bresenhamLine(5, 5, 5, 5)
        assertThat(points).hasSize(1)
        assertThat(points[0]).isEqualTo(Pair(5, 5))
    }

    @Test
    fun `bresenham line — diagonal`() {
        val points = TilePreloader.bresenhamLine(0, 0, 3, 3)
        // Should produce 4 points along the diagonal
        assertThat(points).hasSize(4)
        assertThat(points.first()).isEqualTo(Pair(0, 0))
        assertThat(points.last()).isEqualTo(Pair(3, 3))
    }

    @Test
    fun `bresenham line — all points are unique and connected`() {
        val points = TilePreloader.bresenhamLine(10, 5, 15, 12)
        assertThat(points.size).isAtLeast(2)
        // Each step should change by at most 1 in each axis
        for (i in 1 until points.size) {
            val dx = kotlin.math.abs(points[i].first - points[i - 1].first)
            val dy = kotlin.math.abs(points[i].second - points[i - 1].second)
            assertThat(dx).isAtMost(1)
            assertThat(dy).isAtMost(1)
        }
    }

    @Test
    fun `corridorTileRadius — equator matches legacy approximation`() {
        // At the equator cos(lat)=1, so the new formula must match the old
        // `km / (40075 / 2^zoom)` exactly. Pick km so the floor is ≥2,
        // otherwise both formulas trivially clamp to 1 and the test
        // can't tell them apart.
        val km = 400.0
        val zoom = 8
        val radius = TilePreloader.corridorTileRadius(km, zoom, latDeg = 0.0)
        val legacy = maxOf(1, (km / (40075.0 / (1 shl zoom))).toInt())
        assertThat(radius).isEqualTo(legacy)
        // Sanity: the value is actually ≥2 so the assertion has teeth.
        assertThat(radius).isAtLeast(2)
    }

    @Test
    fun `corridorTileRadius — higher latitude yields more tiles`() {
        // cos(60°)=0.5 → tiles half as wide east-west → a fixed km
        // corridor needs ~2× the tiles. km=500/z8: equator=3, 60°N=6.
        val equator = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 0.0)
        val arctic = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 60.0)
        assertThat(equator).isEqualTo(3)
        assertThat(arctic).isEqualTo(6)
        assertThat(arctic).isEqualTo(equator * 2)
    }

    @Test
    fun `corridorTileRadius — mid-latitude between equator and polar`() {
        // cos(45°)≈0.707, so 45° radius sits between equator and 60°.
        // km=500/z8: equator=3, 45°N=4, 60°N=6.
        val equator = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 0.0)
        val mid = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 45.0)
        val polar = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 60.0)
        assertThat(mid).isGreaterThan(equator)
        assertThat(mid).isLessThan(polar)
    }

    @Test
    fun `corridorTileRadius — floor of one when corridor narrower than a tile`() {
        // A 0.1 km corridor at zoom 8 (tile ~156 km) must still be ≥1.
        val radius = TilePreloader.corridorTileRadius(km = 0.1, zoom = 8, latDeg = 0.0)
        assertThat(radius).isAtLeast(1)
    }

    @Test
    fun `corridorTileRadius — does not blow up near the pole`() {
        // cos(89°)≈0.0175 is tiny but positive; must not divide by zero.
        // With the fix this yields a large radius (~183 for 500 km); with
        // the old equatorial formula it would be 3 — so the gap is real.
        val equator = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 0.0)
        val nearPole = TilePreloader.corridorTileRadius(km = 500.0, zoom = 8, latDeg = 89.0)
        assertThat(nearPole).isAtLeast(1)
        assertThat(nearPole).isGreaterThan(equator)
    }

    @Test
    fun `tileYToLatitude — equator is at the midpoint row`() {
        // Web Mercator: the equator is at tile y = n/2.
        val lat = TilePreloader.tileYToLatitude(y = 1 shl 7, zoom = 8)
        assertThat(lat).isWithin(1e-6).of(0.0)
    }

    @Test
    fun `tileYToLatitude — northern row is positive latitude`() {
        // y=0 is near the north pole (~85°).
        val lat = TilePreloader.tileYToLatitude(y = 0, zoom = 8)
        assertThat(lat).isGreaterThan(80.0)
    }
}
