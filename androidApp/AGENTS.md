# Android App AGENTS

Start here when working on the Android host app.

## Current State

- `androidApp/` owns the Android application plugin, launcher resources, and `MainActivity`.
- The shared Compose UI, GraphQL integration, and Android-specific `expect`/`actual` implementations still live in `composeApp/`.
- This module should stay thin: it is a host shell around the shared KMP client, not a second place for feature logic.

## Read Next

- Root repo guidance: [`../AGENTS.md`](../AGENTS.md)
- Shared frontend guidance: [`../composeApp/AGENTS.md`](../composeApp/AGENTS.md)
- Android entrypoint: [`src/main/kotlin/dk/frankbille/iou/MainActivity.kt`](src/main/kotlin/dk/frankbille/iou/MainActivity.kt)

## Working Guidance

- Keep Android-only app bootstrap, manifest wiring, and launcher resources here.
- Keep UI state, GraphQL code, and cross-platform business logic in `composeApp/` unless Android-only behavior truly requires otherwise.
- If Android app startup changes, validate that `MainActivity` still initializes the shared Android context hooks before rendering `App()`.

## Validation

- Run `./gradlew :androidApp:testDebugUnitTest` for Android host app changes.
- Before finishing, run `./gradlew spotlessCheck`. If it fails, run `./gradlew spotlessApply` and then rerun `./gradlew spotlessCheck`.
