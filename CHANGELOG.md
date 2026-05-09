# Changelog

> **Note**: This file is kept in English for CI/CD tooling compatibility (the release workflow parses it with awk). For a human-readable overview in Chinese, see [ROADMAP.md](ROADMAP.md) and [README.md](README.md).

All notable changes to Stratos will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] — Unreleased (Beta)

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
