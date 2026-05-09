# Getting Started with Stratos

## Installation

Download the latest APK from [GitHub Releases](https://github.com/3aKHP/GPS-Plane/releases) and sideload onto your device. Stratos requires Android 8.0 (API 26) or later.

On first launch, grant location permission when prompted. GPS access is required for all features.

## Quick Start

### Before Your Flight

1. Open Stratos. Confirm the Dashboard shows satellite acquisition.
2. Switch to the **Preload** tab.
3. Enter your departure airport coordinates (tap **Set from GPS** to auto-fill your current location).
4. Enter your destination airport coordinates.
5. Adjust zoom range (defaults 6–12 are adequate for most routes).
6. Tap **Start Download** and wait for completion.
7. Switch to **Map** to verify tiles have loaded along your route.

### During Your Flight

1. Keep Stratos in the foreground. The screen will stay on.
2. **Dashboard** tab: your primary flight instruments (speed, altitude, heading).
3. **Map** tab: moving map with GPS position. Preloaded tiles load from cache; no network needed.
4. Place your phone near the window for best GPS reception.

### After Landing

- Tile cache persists between flights. Re-preload before each trip to refresh coverage.

## Permissions

| Permission | Why Needed |
|---|---|
| Location (Fine + Coarse) | GPS/GNSS satellite positioning |
| Internet | Tile download during preload (not used in flight) |

Stratos does not collect or transmit your location data.
