# Task completion
- Before finishing any task, run `./gradlew spotlessCheck`.
- If `spotlessCheck` fails, run `./gradlew spotlessApply`, then rerun `./gradlew spotlessCheck`.
- Run module-specific verification for the area you changed: backend changes should normally run `./gradlew :server:test`; frontend-only changes should at least run `./gradlew :composeApp:jsTest`, plus platform compile checks when touching expect/actual code.
- If behavior changed, confirm related docs, SDL, shared DTOs, and tests stay aligned.
- Leave Serena memories updated when you learn durable project guidance that future agents will need.