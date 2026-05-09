package com.gpsplane.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gpsplane.app.data.model.GpsData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * ArcGIS Online tile source. ArcGIS Server REST API uses {z}/{y}/{x} URL order
 * (the documented format is tile/{level}/{row}/{col}), while osmdroid defaults
 * to {z}/{x}/{y}. This subclass swaps Y and X to match the ArcGIS convention.
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

private val DefaultTileSource = ArcGISTileSource(
    "ArcGIS-World-Street-Map",
    0, 19, 256, ".jpg",
    arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"
    )
)

@Composable
fun MapScreen(gpsData: GpsData) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapZoom = remember { mutableDoubleStateOf(6.0) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasCentered by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        Configuration.getInstance().apply {
            userAgentValue = "Stratos/0.1.0"
            // Use a fresh cache dir to avoid stale-tile pollution from previous configs
            osmdroidTileCache = context.cacheDir.resolve("osmdroid-v2")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { mapView?.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView?.onPause() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onPause()
            mapView?.onDetach()
            mapView = null
        }
    }

    LaunchedEffect(gpsData.hasFix) {
        if (gpsData.hasFix && !hasCentered) {
            mapView?.post {
                mapView?.controller?.setCenter(GeoPoint(gpsData.latitude, gpsData.longitude))
                mapView?.controller?.setZoom(mapZoom.doubleValue)
                mapView?.invalidate()
            }
            hasCentered = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    setTileSource(DefaultTileSource)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    setUseDataConnection(true) // preloaded tiles served from cache first

                    val startPoint = GeoPoint(
                        if (gpsData.hasFix) gpsData.latitude else 39.9,
                        if (gpsData.hasFix) gpsData.longitude else 116.4
                    )
                    controller.setZoom(mapZoom.doubleValue)
                    controller.setCenter(startPoint)

                    overlays.add(
                        CompassOverlay(ctx, this).apply { enableCompass() }
                    )

                    val locationOverlay = MyLocationNewOverlay(
                        GpsMyLocationProvider(ctx), this
                    )
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                }
            },
            update = { map -> map.invalidate() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    mapZoom.doubleValue = (mapZoom.doubleValue + 1).coerceAtMost(19.0)
                    mapView?.controller?.setZoom(mapZoom.doubleValue)
                }
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
            }
            SmallFloatingActionButton(
                onClick = {
                    mapZoom.doubleValue = (mapZoom.doubleValue - 1).coerceAtLeast(2.0)
                    mapView?.controller?.setZoom(mapZoom.doubleValue)
                }
            ) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
            }
            SmallFloatingActionButton(
                onClick = {
                    if (gpsData.hasFix) {
                        mapView?.controller?.setCenter(
                            GeoPoint(gpsData.latitude, gpsData.longitude)
                        )
                    }
                }
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My Location")
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Text(
                "Map",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
