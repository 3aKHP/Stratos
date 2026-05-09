# Building Stratos

## Prerequisites

- Android Studio (latest stable) or JDK 17 + Android SDK 36
- Gradle 8.11+ (wrapper included)
- Android SDK with build-tools 36+ and platform-tools

## Setup

```bash
git clone <repo-url>
cd GPS-Plane
./gradlew assembleDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Release Builds

Release builds require a signing key:

```bash
# Generate a keystore (one-time)
keytool -genkey -v -keystore app/stratos.keystore \
  -alias stratos -keyalg RSA -keysize 2048 -validity 10000

# Build with signing
./gradlew assembleRelease
```

For CI, store the keystore and passwords as GitHub Secrets:
- `KEYSTORE_BASE64` — base64-encoded keystore file
- `KEYSTORE_PASSWORD` — keystore password
- `KEY_ALIAS` — key alias
- `KEY_PASSWORD` — key password

## Project Structure

```
app/src/main/java/com/gpsplane/app/
├── MainActivity.kt
├── data/           # Repositories + models
├── ui/screen/      # Composable screens
├── ui/theme/       # Material 3 theme
└── util/           # Unit conversions
```
