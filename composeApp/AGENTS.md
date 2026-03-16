# AGENTS

Start here when working on the multiplatform frontend.

## Current State

- `composeApp/` is the shared Kotlin Multiplatform client library used by the Android host app plus the iOS, JS, and Wasm targets.
- `androidApp/` now owns the Android application plugin, launcher resources, and `MainActivity`; `composeApp/` remains the shared UI and platform integration layer.
- The UI is no longer the default template. It now contains a dashboard-style product direction with household finance/task sections, but it is still mostly sample-data driven.
- GraphQL integration now loads the broader `ViewerDashboard` payload and maps it into the dashboard UI state.
- Parent authentication now has a basic login/register flow with locally persisted JWT storage and a loading bootstrap before the dashboard appears.
- Subscriptions are intentionally deferred. The current client only covers query + cache behavior.

## Read Next

- Root repo guidance: [`../AGENTS.md`](../AGENTS.md)
- Product/domain source of truth: [`../SPEC.md`](../SPEC.md)
- Shared app entrypoint: [`src/commonMain/kotlin/dk/frankbille/iou/App.kt`](src/commonMain/kotlin/dk/frankbille/iou/App.kt)
- Dashboard UI: [`src/commonMain/kotlin/dk/frankbille/iou/dashboard/`](src/commonMain/kotlin/dk/frankbille/iou/dashboard/)
- Session/auth state machine: [`src/commonMain/kotlin/dk/frankbille/iou/session/`](src/commonMain/kotlin/dk/frankbille/iou/session/)
- GraphQL repository and cache hydration: [`src/commonMain/kotlin/dk/frankbille/iou/graphql/DashboardRepository.kt`](src/commonMain/kotlin/dk/frankbille/iou/graphql/DashboardRepository.kt)
- Apollo query definitions: [`src/commonMain/graphql/`](src/commonMain/graphql/)

## Frontend Decisions So Far

- Apollo Kotlin is the chosen GraphQL client library.
- The intended data model follows the `SPEC.md` rule to front-load family data and read from cache during screen transitions.
- The current implementation uses Apollo normalized in-memory cache plus platform persistence for a serialized query snapshot that can rehydrate Apollo on cold start.
- The current query focuses on `ViewerDashboard.graphql` and maps the first accessible family into dashboard-shaped UI models.
- The dashboard UI state is deliberately separate from GraphQL response models so larger queries can later map into screen-shaped state.

## Caching and Persistence

- Web persists the serialized family snapshot in browser local storage.
- Android persists the snapshot in the app files directory. The Android host app's `MainActivity` initializes the application context needed by that storage layer before Compose starts.
- iOS persists the snapshot in `NSUserDefaults`.
- Persistence is keyed by GraphQL server URL and JWT hash so cached data is only rehydrated for the matching session context.
- This is a POC cache strategy, not the final secure-storage story. The JWT itself is now persisted locally through a separate platform-specific storage abstraction.

## Authentication Notes

- The Apollo client adds `Authorization: Bearer <jwt>` on requests when a token is present.
- Parent login and registration use the server's REST auth endpoints, then hydrate the GraphQL dashboard after the JWT is stored locally.
- The GraphQL server address is defined in app config rather than entered in the UI.
- There is still no token refresh flow or secure credential storage in `composeApp/` yet.
- If auth behavior changes, update both the REST auth client and the Apollo interceptor path, plus the persisted-cache keying assumptions.

## Working Guidance

- Keep UI models in `composeApp/` shaped for presentation and map GraphQL models into them instead of letting generated Apollo models leak through the composables.
- Prefer small vertical slices when expanding GraphQL usage: query, repository mapping, cache behavior, then UI hookup.
- Preserve the cache-first navigation direction from `SPEC.md`. New screens should prefer cache-backed reads after the family payload has been loaded.
- When extending GraphQL behavior, keep the generated query files and the consuming Kotlin code in the same change so the diff stays reviewable.
- If you add more persisted Apollo data, reuse the platform snapshot abstraction unless there is a strong reason to move to a fuller storage layer.
- Do not assume the JS/browser behavior proves mobile behavior. Validate Android and iOS source sets when touching `expect`/`actual` code.

## Validation

- For frontend-only changes, `./gradlew :composeApp:jsTest` is the fastest baseline check.
- When touching Android host or Android-specific frontend behavior, also run `./gradlew :androidApp:testDebugUnitTest`.
- When touching iOS `expect`/`actual` code, also run `./gradlew :composeApp:compileKotlinIosSimulatorArm64`.
- Before finishing, run `./gradlew spotlessCheck`. If it fails, run `./gradlew spotlessApply` and then rerun `./gradlew spotlessCheck`.
