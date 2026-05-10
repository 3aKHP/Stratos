# Changelog

> **Note**: This file is kept in English for CI/CD tooling compatibility (the release workflow parses it with awk). For a human-readable overview in Chinese, see [ROADMAP.md](ROADMAP.md) and [README.md](README.md).

All notable changes to Stratos will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.2.0-alpha.4] — 2026-05-10

Fourth pre-release of v0.2.0. Moves GPS and sensor subscriptions into
a foreground service so flight data keeps logging when the screen is
off, and adds automatic GPX track recording.

### Added
- **Background GPS tracking.** A foreground service now owns the GPS
  and sensor subscriptions for the whole app, with a persistent
  notification showing current phase (GROUND / T+HH:MM:SS) and a Stop
  action. Tracking keeps running when the app is backgrounded or the
  screen turns off — the main passenger-use gap since v0.1.
- **Automatic GPX track recording.** On takeoff (FlightTimer transition
  into AIRBORNE), Stratos opens a new `.gpx` file under app storage
  and writes one point per GPS sample until landing. Files are named
  `YYYYMMDD-HHMMSS.gpx` in UTC and stored in
  `filesDir/tracks/` (covered by Google Auto Backup; each flight is
  well under the 25 MB per-app cap).
- **Auto-record toggle.** The settings sheet gains a Recording
  section; toggling off mid-flight closes the current file cleanly
  and toggling back on starts a new one at the next sample.

### Changed
- **MainActivity data flow** is now sourced from a bound service, not
  from direct `combine(gps, att, env)` flow collection. This unblocks
  screen-off tracking and lets future features (track playback,
  sharing) share the same state.
- **`User-Agent` version** now derives from `BuildConfig.VERSION_NAME`
  across `TilePreloader`, `MapScreen`, and `DownloadScreen` — the
  three-place sync step is gone.

## [0.2.0-alpha.3] — 2026-05-10

Third pre-release of v0.2.0. Fixes a long-standing correctness bug in
the sky plot: the compass was canvas-fixed, so its N/S/E/W labels and
satellite positions were only true when the phone happened to point
north. The plot now tracks physical heading.

### Added
- **Heading-stabilized sky plot.** Top of the dial now follows flight
  direction (TRK-UP) in motion and phone heading (HDG-UP) when
  stationary; satellites, cardinals, and tick marks rotate with the
  dial so they match the real sky. Switchover at 1.5 m/s.
- **"NO HEADING" degrade label.** Shown when the phone is stationary
  and the compass sensor isn't producing valid data; the dial falls
  back to NORTH-UP in that case.
- **Magnetic-north marker.** A small red "N" sits clockwise from the
  true-north "N" by the local magnetic declination, making the offset
  visible at a glance.
- **Meaningful phone orientation fan.** Under TRK-UP the existing grey
  fan now shows where the phone points relative to the flight
  direction — a useful cue for window-seat users reconciling what
  they see out the window with their heading. Fan is hidden under
  HDG-UP (where it would always point straight up) and when there's
  no compass.

### Removed
- **Red GPS track needle.** Obsoleted by TRK-UP (the top of the dial
  already IS the track direction) and was actively misleading under
  the old canvas-fixed layout.

## [0.2.0-alpha.2] — 2026-05-10

Second pre-release of v0.2.0. Adds navigation and flight-status
readouts to the top bar, and tightens the dashboard layout.

### Added
- **ZULU clock** — UTC time of day in the top bar, ticking once a
  second (`HH:MM:SSZ`).
- **Flight timer** — two-state machine (GROUND / AIRBORNE) inferred
  from GPS. Reads `GROUND` when taxiing or idle; switches to
  `T+HH:MM:SS` 10 seconds after speed exceeds 150 kn above 3000 ft,
  and reverts to `GROUND` 30 seconds after dropping below 80 kn.
- **Magnetic heading** — `TRACK` now defaults to magnetic north using
  the platform's WMM model via `android.hardware.GeomagneticField`.
  The settings sheet adds a *Heading Reference* row to switch between
  `MAG` and `TRUE`; the label updates to `TRACK·M` or `TRACK·T`.
- `MagneticDeclination` helper (6 tests) and `FlightTimer` pure-function
  state machine (12 tests).

### Changed
- **Heading default is now magnetic.** Previous releases showed true
  track; existing users will see `TRACK·M` on first launch and can
  switch back via settings.
- Sky plot cardinal labels (N/S/E/W) re-centered — they were offset
  7 dp to the left, which was most visible as N/S drifting off the
  tick column below them.
- Signal bars align with the sky plot's 16 dp horizontal inset; the
  Galileo `E` chip used to overrun the plot's `S` label when many
  constellations were visible.

## [0.2.0-alpha.1] — 2026-05-10

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

## [0.1.2] — 2026-05-10

### Changed
- GPS and sensor flows now stop when the activity is backgrounded and resume on return (`flowWithLifecycle(STARTED)`), avoiding silent GPS drain off-screen
- Tile cache moved from `filesDir` to `noBackupFilesDir` so preloaded tiles are excluded from Google Auto Backup (25 MB per-app cap). Tiles preloaded on 0.1.1 (under `filesDir/osmdroid-v2/`) are deleted on first launch; re-run the preloader to restore coverage.
- Repositories refactored: extracted `VerticalSpeedFilter`, `AttitudeMath`, and `SatelliteStats` as pure helpers for unit testing

### Added
- Unit tests for `VerticalSpeedFilter`, `AttitudeMath`, and `SatelliteStats` (13 new tests)

## [0.1.1] — 2026-05-10

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
