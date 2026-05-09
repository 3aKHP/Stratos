# Testing Stratos

## Unit Tests

```bash
./gradlew test
```

Tests live in `app/src/test/`. JUnit 4 + Google Truth for assertions. No Android device required — these run on the JVM.

| Test Class | Covers |
|---|---|
| `UnitConverterTest` | Unit conversions, DMS formatting, ISA sound speed, Mach |
| `TilePreloaderTest` | Great-circle distance, spherical interpolation, Bresenham line |

## Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

*Not yet implemented.* Instrumentation tests require a device or emulator and will cover:
- GPS data flow from `GpsRepository`
- MapView rendering with tile cache
- UI interaction (tab navigation, unit config, preload flow)

## Manual Testing

For GPS-dependent features, test on a physical device outdoors or near a window:

1. **Dashboard**: verify fix acquisition, instrument values, sky plot rendering
2. **Map**: verify tiles load, position marker moves, pan/zoom works
3. **Preload**: enter coordinates, start download, verify progress and tile count
4. **Offline**: enable airplane mode, confirm map uses cached tiles
