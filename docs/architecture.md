# Architecture

## Data Flow

```
┌─────────────┐  ┌──────────────────┐
│ GpsRepository│  │AttitudeRepository│
│ (GPS_PROVIDER)│  │(ROTATION_VECTOR) │
│  GnssStatus  │  │ LINEAR_ACCEL    │
└──────┬───────┘  └────────┬─────────┘
       │ Flow<GpsData>     │ Flow<AttitudeData>
       └─────────┬─────────┘
                 │ combine { }
          ┌──────┴──────┐
          │  MainScreen  │
          └──────┬──────┘
                 │ State<GpsData, AttitudeData>
    ┌────────────┼────────────┐
    │            │            │
┌───┴───┐  ┌────┴────┐  ┌───┴────────┐
│GpsScr.│  │MapScr.  │  │DownloadScr.│
└───────┘  └─────────┘  └────────────┘
```

## Component Responsibilities

### GpsRepository
- Wraps `LocationManager.requestLocationUpdates(GPS_PROVIDER, 1s, 0m)`
- `GnssStatus.Callback` for per-satellite SNR/azimuth/elevation
- Low-pass filtered vertical speed from GPS altitude deltas
- Session TTFF tracking via `SystemClock.elapsedRealtime()`
- Lifecycle: callback registered in `callbackFlow`, cleaned in `awaitClose`

### AttitudeRepository
- `SensorManager` for `TYPE_ROTATION_VECTOR` (azimuth/pitch/roll) and `TYPE_LINEAR_ACCELERATION`
- `SENSOR_DELAY_GAME` (~20ms) update rate
- Emits `AttitudeData` on each sensor event

### TilePreloader
- Great-circle route sampling → tile coordinate enumeration → corridor expansion
- Concurrent HTTP download (8 parallel, `Semaphore`-gated) to osmdroid filesystem cache
- Cache format: `{tileSourceName}/{z}/{x}/{y}.tile` — directly readable by `MapTileFilesystemProvider`

### MapScreen
- osmdroid `MapView` embedded via `AndroidView`
- ArcGIS tile source with `{z}/{y}/{x}` URL order (override of `OnlineTileSourceBase.getTileURLString`)
- `MyLocationNewOverlay` for position indicator
- User-controlled pan/zoom; initial auto-center on first fix only

### GpsScreen
- Non-scrollable single-screen layout
- Canvas-drawn sky plot with compass ring, bearing needle, orientation fan, artificial horizon, satellite dots
- Compact per-constellation signal bars
- Dual-unit instrument cells (Speed, Altitude, V/S, Heading/Track)

## Key Design Decisions

**GPS_PROVIDER over FusedLocationProvider**: at altitude, WiFi/cell tower triangulation is unavailable. Raw GPS gives us direct control and access to `GnssStatus` for satellite metadata.

**GPS altitude for vertical speed over barometer**: commercial aircraft cabins are pressurized to ~8,000 ft equivalent regardless of actual altitude. Barometric vertical speed would read near-zero during cruise. GPS altitude differencing (with EMA filter, α=0.3) is noisy but directionally correct.

**Widget-less, non-scrollable dashboard**: in-cockpit use requires single-glance readability. Every pixel of vertical space is budgeted — the sky plot fills available height via `Modifier.weight(1f)`.

**ArcGIS tile source**: MAPNIK (tile.openstreetmap.org) is blocked in China. ArcGIS Online World Street Map is accessible and provides adequate map detail for route visualization.

**No tile server in flight**: tiles must be preloaded before takeoff. The preloader writes directly to osmdroid's filesystem cache format; the map provider chain hits cache before network.
