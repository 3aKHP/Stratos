# Contributing to Stratos

## Code Style

- Kotlin, following [official coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Compose UI: prefer `@Composable` functions that take data as parameters and emit no side effects.
- Repository pattern: data sources expose `Flow<T>` via `callbackFlow`; cleanup in `awaitClose`.
- No comments that explain WHAT the code does; use comments only for WHY (non-obvious constraints, workarounds).

## Commit Messages

- One line summary under 72 characters, imperative mood ("Fix", "Add", not "Fixed" or "Added").
- Body (optional): bullet points for motivation and notable side effects.
- Reference issue numbers when applicable.

## Branch Strategy

- `main` — stable, buildable at all times.
- Feature branches: `feature/<name>` or `fix/<name>`, merged via PR.

## Testing

- Unit tests for pure functions (especially `UnitConverter`, `TilePreloader` geometry).
- Run: `./gradlew test`.
- Instrumentation tests on a physical device or emulator for GPS/sensor-dependent code.
