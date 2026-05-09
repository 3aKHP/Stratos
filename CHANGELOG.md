# Changelog

> **Note**: This file is kept in English for CI/CD tooling compatibility (the release workflow parses it with awk). For a human-readable overview in Chinese, see [ROADMAP.md](ROADMAP.md) and [README.md](README.md).

All notable changes to Stratos will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.2.0-alpha.1] — Unreleased

First pre-release of the v0.2.0 Cockpit & Sensors milestone. Introduces
barometer support and fleshes out the motion-sensor readouts.

### Added
- **Load factor (g)** — aviation-standard load-factor readout from the raw
  accelerometer (reads ~1g at rest, >1g in pull-ups). Coexists with the
  existing `ACC` reading, which stays as the gravity-removed airframe
  motion magnitude.
- **Turn rate** — yaw rate in °/s from the gyroscope z-axis, positive =
  right turn. Displayed in the top metric row.
- **Cabin altitude / pressure** — derived from the device barometer
  (`TYPE_PRESSURE`) via ISA. Reads "no baro" on devices without a pressure
  sensor.
- **Static altitude / pressure** — the ISA-modelled outside-air pressure
  at the GPS-reported altitude; always available when a fix is present.
- `PressureMath` (ISA pressure ↔ altitude) with 6 unit tests.
- `EnvironmentRepository` for environmental sensors; added alongside
  `GpsRepository` and `AttitudeRepository` in the main data flow.

### Changed
- `AttitudeMath.linearAccelerationToG` renamed to `magnitudeInG` — same
  math, now reused for both the linear-acceleration and raw-accelerometer
  inputs.
- `AttitudeData` gains `loadFactorG` and `turnRateDegPerSec` fields.
- Dashboard top metric row expands from 4 to 6 cells
  (Mach, Pitch, Roll, Acc, Load, Turn). Tight but readable in portrait.

## [0.1.2] — Unreleased

### Changed
- GPS and sensor flows now stop when the activity is backgrounded and resume on return (`flowWithLifecycle(STARTED)`), avoiding silent GPS drain off-screen
- Tile cache moved from `filesDir` to `noBackupFilesDir` so preloaded tiles are excluded from Google Auto Backup (25 MB per-app cap). Tiles preloaded on 0.1.1 (under `filesDir/osmdroid-v2/`) are deleted on first launch; re-run the preloader to restore coverage.
- Repositories refactored: extracted `VerticalSpeedFilter`, `AttitudeMath`, and `SatelliteStats` as pure helpers for unit testing

### Added
- Unit tests for `VerticalSpeedFilter`, `AttitudeMath`, and `SatelliteStats` (13 new tests)

## [0.1.1] — Unreleased

### Changed
- Consolidate ArcGIS tile source into `data/tiles/ArcGISWorldStreetMap` shared by map and preloader
- Tile cache moved from `cacheDir` to `filesDir` so preloaded tiles survive system cache eviction
- TilePreloader: Semaphore now actually gates concurrency (removed batched `awaitAll`)
- TilePreloader: send `User-Agent` header and disconnect HTTP connections after each tile
- TilePreloader: throttle progress callbacks to main thread (≤100ms interval)

### Removed
- `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` permissions — not used yet, will return with the v0.2.0 foreground service
- `WRITE_EXTERNAL_STORAGE` permission — osmdroid now uses internal app storage (`filesDir`)

## [0.1.0] — Beta

### Added
- GPS Dashboard with avionics-style instrument panel (speed, altitude, heading, vertical speed)
- Dual-unit display with configurable units per metric via bottom sheet
- Mach number, pitch, roll, and acceleration readouts
- Real-time satellite sky plot with compass overlay and artificial horizon
- Bearing needle (GPS track from center) and compass fan indicator
- Offline map via osmdroid + ArcGIS tile source
- Tile preloader with great-circle route corridor enumeration and concurrent download
- GNSS satellite signal quality panel (SNR bars, per-constellation grouping)
- UnitConverter with ISA standard atmosphere Mach calculation
- AttitudeRepository (ROTATION_VECTOR + LINEAR_ACCELERATION sensor fusion)
- Dark-only Material 3 theme
- Keep-screen-on flag for cockpit use
