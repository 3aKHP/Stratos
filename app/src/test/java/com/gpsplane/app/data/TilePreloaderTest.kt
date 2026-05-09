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
}
