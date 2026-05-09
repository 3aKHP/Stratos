# Stratos

**GPS flight instrument for commercial aviation.**

Stratos turns your phone into a cockpit-style flight display. Built for window-seat passengers who want to see real-time speed, altitude, heading, vertical velocity, satellite constellations, and position on an offline map — all in one screen, without network.

## Features

- **Avionics dashboard** — Speed, altitude, vertical speed, heading, Mach number, pitch/roll, acceleration. Dual-unit display (kn/kmh, ft/m, etc.), configurable per metric.
- **Offline map with route preloading** — ArcGIS-based moving map. Pre-download tiles along a great-circle route corridor before takeoff; works entirely offline in flight.
- **Satellite sky plot** — Real-time polar chart of visible GNSS satellites (GPS, GLONASS, BeiDou, Galileo, QZSS) with SNR bars, azimuth/elevation, and constellation colors.
- **Compass & attitude** — Bearing needle (GPS track) and phone-compass fan indicator overlaid on the sky plot. Artificial horizon for pitch/roll.
- **GNSS constellation breakdown** — Per-constellation satellite counts and signal quality.
- **Unit customization** — Pick two units for each measurement; display both simultaneously. Aviation defaults pre-selected.

## Screenshots

> *Coming in v0.2.0*

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Maps | osmdroid 6.1.20 (ArcGIS tile source) |
| GPS | `LocationManager` (GPS_PROVIDER, 1 Hz) |
| Sensors | `SensorManager` (ROTATION_VECTOR, LINEAR_ACCELERATION) |
| Async | Kotlin Coroutines + Flow |
| Build | AGP 8.13, Kotlin 2.1, Gradle 8.11 |

## Requirements

- Android 8.0+ (API 26)
- Device with GPS/GNSS hardware
- For full experience: compass, accelerometer, gyroscope

## Setup

```bash
git clone <repo-url>
cd GPS-Plane
# Open in Android Studio, or:
./gradlew assembleDebug
```

Signing config is not included in source. Place your keystore at `app/stratos.keystore` and configure `app/build.gradle.kts` with a `signingConfig` block for release builds.

## Project Structure

```
app/src/main/java/com/gpsplane/app/
├── MainActivity.kt              # Entry point, tab navigation, permission gating
├── data/
│   ├── GpsRepository.kt         # GPS data via LocationManager + GnssStatus
│   ├── AttitudeRepository.kt    # IMU data via SensorManager
│   ├── TilePreloader.kt         # Offline tile download & cache
│   └── model/
│       ├── GpsData.kt           # GPS snapshot data class
│       ├── AttitudeData.kt      # IMU snapshot data class
│       └── SatelliteInfo.kt     # Per-satellite GNSS metadata
├── ui/
│   ├── screen/
│   │   ├── GpsScreen.kt         # Dashboard (instruments, sky plot, signals)
│   │   ├── MapScreen.kt         # Offline map with position overlay
│   │   └── DownloadScreen.kt    # Tile preload UI
│   └── theme/
│       └── Theme.kt             # Material 3 dark theme
└── util/
    └── UnitConverter.kt         # Unit conversions + Mach
```

## License

Source code is currently unlicensed. A license will be chosen before the first public release.
