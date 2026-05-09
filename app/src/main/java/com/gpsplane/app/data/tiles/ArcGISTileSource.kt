package com.gpsplane.app.data.tiles

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * ArcGIS Server REST API uses {z}/{y}/{x} URL order (documented as
 * tile/{level}/{row}/{col}), while osmdroid defaults to {z}/{x}/{y}.
 * This subclass swaps Y and X to match the ArcGIS convention.
 *
 * The tile source name is load-bearing: [TilePreloader] writes tiles into
 * `{cacheDir}/{name()}/{z}/{x}/{y}.tile`, and osmdroid's
 * MapTileFilesystemProvider reads from the same path. Renaming silently
 * invalidates preloaded caches.
 */
private class ArcGISTileSource(
    name: String, zoomMin: Int, zoomMax: Int,
    tileSize: Int, imageExt: String, baseUrls: Array<String>
) : OnlineTileSourceBase(name, zoomMin, zoomMax, tileSize, imageExt, baseUrls) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        return getBaseUrl() +
            MapTileIndex.getZoom(pMapTileIndex).toString() + "/" +
            MapTileIndex.getY(pMapTileIndex).toString() + "/" +
            MapTileIndex.getX(pMapTileIndex).toString() +
            mImageFilenameEnding
    }
}

val ArcGISWorldStreetMap: OnlineTileSourceBase = ArcGISTileSource(
    "ArcGIS-World-Street-Map",
    0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
)
