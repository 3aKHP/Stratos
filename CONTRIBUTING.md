# Contributing to Stratos

Thanks for your interest. This file documents the project's expectations so contributions land cleanly.

## Code Style

### Language & layering

- Kotlin, following the [official coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Allowed dependency directions: **`ui → data → data/model`**. Do not:
  - call `LocationManager` / `SensorManager` from a `@Composable`
  - put computation logic in `data/model/*.kt` (keep them data-only)
  - let `util/` depend on `data/`
- Repositories only own callback → Flow plumbing. Any math/filtering/statistics must live in a separate `object` or `class` (see `VerticalSpeedFilter`, `AttitudeMath`, `SatelliteStats`, `PressureMath`, `EmaFilter`).
- Composable functions take data-class parameters, callbacks, and `Modifier` — nothing else. Never a `Repository`.

### File size & single responsibility

- One file, one concern. Watch out at ~300 lines; a file above that usually has at least two responsibilities.
- UI formatting helpers (`fmtSpd`, `fmtAlt`, …) and enums (`SpeedUnit`, `AltUnit`, …) belong in `ui/format/` once the number grows. Small private Composable helpers belong in `ui/component/` for the same reason.
- Data sources, pure helpers, and UI should not share a file.

### When to extract

Extract immediately (don't defer to "next PR") when any of these happen:

- The same formula appears in two places.
- A function or Composable passes ~50 lines or nests more than three levels.
- A block of logic has a "state + update + query" shape — it wants to be a class.
- A block needs unit tests — it wants to be a pure function (`object`) or a stateful helper (`class`).

### When not to extract

Don't abstract for its own sake. Single-use, under ~10 lines, semantically clear inline code is fine. Test: *"If I rewrote this tomorrow for a test or reuse, would the current shape fight me?"*

### Comments & documentation

- Public APIs get KDoc. Document **what and why**, not **how**.
- No comments that restate the code (`// increment i`). Use comments for non-obvious constraints, sign conventions, workarounds, or any decision that would surprise a future reader (see `AttitudeRepository`'s gyroscope z-axis flip, `TilePreloader`'s corridor approximation).

### Boy Scout rule, with a boundary

Leave code a little cleaner than you found it — rename a local, untangle a nested `if`, drop a dead import. But:

- **Don't fold unrelated refactors into a feature or bugfix PR.** If you notice a messy file that needs restructuring, open a dedicated `refactor(...)` PR for it.
- Refactor PRs should keep their diff narrow to the move/rename itself — don't bundle new features into a cleanup.

### Anti-patterns

These are non-negotiable; don't ship any of them:

- Thousand-line UI or Activity files.
- Repositories that hold UI state or format strings.
- A sprawling `Utils.kt` / `Helper.kt` — split by theme (`UnitConverter`, `AttitudeMath`, …).
- Duplicated constants across files — promote to a `companion object` or top-level `const val`.
- Cross-layer calls (a `@Composable` opening a `SensorManager`, a repository reading Compose state).
- Composables that branch wildly into unrelated UIs based on a single flag — split them.

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/).

Format: `<type>(<scope>): <subject>`

- **type**: `feat` / `fix` / `refactor` / `docs` / `chore` / `test` / `style` / `perf`
- **scope** (common here): `data` / `sensors` / `tiles` / `ui` / `lifecycle` / `release` / `ci`
- **subject**: lowercase, imperative, ≤72 chars, no trailing period
- **body** (optional): explain motivation and notable side effects; reference issue/PR numbers when relevant

One logical change per commit. Every intermediate commit on a feature branch should compile and test cleanly (bisect-friendly).

## Branch Strategy

- `main` is always buildable.
- Feature branches: `<type>/v<version>-<topic>` — e.g. `feat/v0.2.0-alpha.1-sensors`, `chore/v0.1.2-lifecycle-and-testability`.
- Small chore/docs fixes (typos, version-number oversights right after a merge) may go directly to `main`. Anything at `feat` / `fix` / `refactor` level goes through a PR.

## Pull Requests

Structure the PR description with three sections:

1. **Summary** — bullet list of commits or themes, what changed and why.
2. **Test plan** — what you ran (`./gradlew test`, `assembleDebug`) and what you verified on a real device if applicable.
3. **Follow-ups** — anything deliberately out of scope.

### Real-device verification

A real device run is required before merging any PR that:

- touches UI (new Composables, layout dimensions)
- changes the Manifest (permissions, activity flags)
- adds a new sensor or `LocationManager` subscription
- changes storage paths (`cacheDir` / `filesDir` / `noBackupFilesDir`)
- changes lifecycle-related code

Debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Testing

- New pure-function helpers in `data/*.kt` **must** come with unit tests (`app/src/test/`).
- JUnit 4 + Truth. JVM only — no Android device needed for unit tests.
- When a test touches Android platform constants (e.g. `GnssStatus.CONSTELLATION_*`), hard-code the integer values with a comment — the stub `android.jar` returns 0 for every constant.
- Do **not** enable `testOptions.unitTests.isReturnDefaultValues = true`. It silently turns accidental Android-API calls in tests into `0` / `null` / `false` and hides bugs.
- Instrumentation tests are not yet set up; run device-dependent checks manually per the real-device checklist above.

Commands:
```bash
./gradlew test            # all unit tests
./gradlew assembleDebug   # debug APK
```

## Versioning & CHANGELOG

- [Semantic Versioning](https://semver.org/). Pre-releases use `-alpha.N` / `-beta.N` / `-rc.N`.
- Tags are `v`-prefixed (`v0.2.0-alpha.1`). Tags containing `-` trigger a GitHub prerelease via `.github/workflows/release.yml`.
- A version bump must update **all** of:
  - `app/build.gradle.kts` (`versionCode` + `versionName`)
  - `app/src/main/java/com/gpsplane/app/data/TilePreloader.kt` (`USER_AGENT`)
  - `app/src/main/java/com/gpsplane/app/ui/screen/MapScreen.kt` (`userAgentValue`)
  - `app/src/main/java/com/gpsplane/app/ui/screen/DownloadScreen.kt` (`userAgentValue`)
  - `CHANGELOG.md`

### CHANGELOG

Follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). **Written in English** — the release workflow's awk script depends on the `## [VERSION]` header format.

- Describe user-facing changes, not commit metadata. Do not copy commit subjects verbatim.
- Categories: **Added** / **Changed** / **Deprecated** / **Removed** / **Fixed** / **Security** — only include those that have entries.
- During development, append to `## [Unreleased]`. When tagging, rename it to `## [X.Y.Z] — YYYY-MM-DD` and insert a fresh empty `## [Unreleased]` above.
