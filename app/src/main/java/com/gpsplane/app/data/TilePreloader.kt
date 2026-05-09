package com.gpsplane.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class TilePreloader(
    private val cacheDir: File,
    private val tileSource: OnlineTileSourceBase
) {

    data class Progress(val downloaded: Int, val total: Int, val failed: Int)

    /**
     * Download all tiles along the great-circle route corridor.
     * The order of arguments is [departure] → [destination] for display,
     * but the route is symmetric — direction doesn't matter for tile enumeration.
     */
    suspend fun preload(
        departure: GeoPoint,
        destination: GeoPoint,
        zoomMin: Int,
        zoomMax: Int,
        corridorKm: Double,
        onProgress: suspend (Progress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allTiles = mutableSetOf<Long>()
            for (zoom in zoomMin..zoomMax) {
                val routeTiles = routeToTileLine(departure, destination, zoom)
                val corridorTiles = expandCorridor(routeTiles, corridorKm, zoom)
                allTiles.addAll(corridorTiles)
            }

            val total = allTiles.size
            var downloaded = 0
            var failed = 0

            val semaphore = Semaphore(MAX_CONCURRENT)
            val tileList = allTiles.toList()

            tileList.chunked(MAX_CONCURRENT).forEach { batch ->
                batch.map { tileIndex ->
                    async {
                        semaphore.withPermit {
                            val ok = downloadTile(tileIndex)
                            synchronized(this@TilePreloader) {
                                if (ok) downloaded++ else failed++
                            }
                            withContext(Dispatchers.Main) {
                                onProgress(Progress(downloaded + failed, total, failed))
                            }
                        }
                    }
                }.awaitAll()
            }

            Result.success(downloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Total tile count for the route (for pre-download estimation). */
    fun estimateTileCount(
        departure: GeoPoint,
        destination: GeoPoint,
        zoomMin: Int,
        zoomMax: Int,
        corridorKm: Double
    ): Int {
        val allTiles = mutableSetOf<Long>()
        for (zoom in zoomMin..zoomMax) {
            val routeTiles = routeToTileLine(departure, destination, zoom)
            val corridorTiles = expandCorridor(routeTiles, corridorKm, zoom)
            allTiles.addAll(corridorTiles)
        }
        return allTiles.size
    }

    // ---- tile enumeration ----

    private fun routeToTileLine(
        from: GeoPoint, to: GeoPoint, zoom: Int
    ): Set<Pair<Int, Int>> {
        val tiles = mutableSetOf<Pair<Int, Int>>()
        val distKm = greatCircleDistance(from, to)
        val stepKm = 10.0
        val steps = maxOf(2, (distKm / stepKm).toInt())

        var prevX = -1
        var prevY = -1
        for (i in 0..steps) {
            val f = i.toDouble() / steps
            val pt = interpolate(from, to, f)
            val (x, y) = latLonToTile(pt.latitude, pt.longitude, zoom)
            if (i > 0) {
                bresenhamLine(prevX, prevY, x, y).forEach { tiles.add(it) }
            } else {
                tiles.add(Pair(x, y))
            }
            prevX = x
            prevY = y
        }
        return tiles
    }

    private fun expandCorridor(
        routeTiles: Set<Pair<Int, Int>>, corridorKm: Double, zoom: Int
    ): Set<Long> {
        val result = mutableSetOf<Long>()
        val radius = corridorTiles(corridorKm, zoom)
        for ((cx, cy) in routeTiles) {
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    val x = cx + dx
                    val y = cy + dy
                    if (x >= 0 && y >= 0 && x < (1 shl zoom) && y < (1 shl zoom)) {
                        result.add(MapTileIndex.getTileIndex(zoom, x, y))
                    }
                }
            }
        }
        return result
    }

    // ---- tile download ----

    private fun downloadTile(tileIndex: Long): Boolean {
        return try {
            val url = URL(tileSource.getTileURLString(tileIndex))
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return false

            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size < 100) return false // too small to be a real tile

            val tileFile = cacheFile(tileIndex)
            tileFile.parentFile?.mkdirs()
            tileFile.writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun cacheFile(tileIndex: Long): File {
        val zoom = MapTileIndex.getZoom(tileIndex)
        val x = MapTileIndex.getX(tileIndex)
        val y = MapTileIndex.getY(tileIndex)
        return File(cacheDir, "${tileSource.name()}/$zoom/$x/$y.tile")
    }

    // ---- geometry helpers ----

    private fun latLonToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n)
            .toInt().coerceIn(0, n - 1)
        return Pair(x, y)
    }

    private fun corridorTiles(km: Double, zoom: Int): Int {
        // Approximate: 1 tile at equator ≈ (40075 / 2^zoom) km
        val tileWidthKm = 40075.0 / (1 shl zoom)
        return maxOf(1, (km / tileWidthKm).toInt())
    }

    companion object {
        private const val MAX_CONCURRENT = 8
        private const val EARTH_RADIUS_KM = 6371.0

        /** Great-circle distance in km (Haversine). */
        fun greatCircleDistance(a: GeoPoint, b: GeoPoint): Double {
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLon = Math.toRadians(b.longitude - a.longitude)
            val lat1 = Math.toRadians(a.latitude)
            val lat2 = Math.toRadians(b.latitude)
            val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
        }

        /** Point at fraction [f] (0..1) along the great circle from [a] to [b]. */
        fun interpolate(a: GeoPoint, b: GeoPoint, f: Double): GeoPoint {
            val lat1 = Math.toRadians(a.latitude)
            val lon1 = Math.toRadians(a.longitude)
            val lat2 = Math.toRadians(b.latitude)
            val lon2 = Math.toRadians(b.longitude)

            val d = 2 * asin(sqrt(
                sin((lat2 - lat1) / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).pow(2)
            ))
            // When points are identical (or extremely close), d ≈ 0 → avoid div-by-zero
            if (d < 1e-10) return a

            val a1 = sin((1 - f) * d) / sin(d)
            val a2 = sin(f * d) / sin(d)

            val x = a1 * cos(lat1) * cos(lon1) + a2 * cos(lat2) * cos(lon2)
            val y = a1 * cos(lat1) * sin(lon1) + a2 * cos(lat2) * sin(lon2)
            val z = a1 * sin(lat1) + a2 * sin(lat2)

            return GeoPoint(
                Math.toDegrees(atan2(z, sqrt(x * x + y * y))),
                Math.toDegrees(atan2(y, x))
            )
        }

        /** Bresenham line in tile coordinates. */
        fun bresenhamLine(x1: Int, y1: Int, x2: Int, y2: Int): List<Pair<Int, Int>> {
            val points = mutableListOf<Pair<Int, Int>>()
            var x = x1; var y = y1
            val dx = abs(x2 - x1); val dy = -abs(y2 - y1)
            val sx = if (x1 < x2) 1 else -1
            val sy = if (y1 < y2) 1 else -1
            var err = dx + dy
            while (true) {
                points.add(Pair(x, y))
                if (x == x2 && y == y2) break
                val e2 = 2 * err
                if (e2 >= dy) { err += dy; x += sx }
                if (e2 <= dx) { err += dx; y += sy }
            }
            return points
        }
    }
}
