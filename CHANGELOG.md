# Changelog

> **Note**: This file is kept in English for CI/CD tooling compatibility (the release workflow parses it with awk). For a human-readable overview in Chinese, see [ROADMAP.md](ROADMAP.md) and [README.md](README.md).

All notable changes to Stratos will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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
