# AGENTS

Start here when working in this repository.

## Project State

- `SPEC.md` is the behavioral source of truth for the domain model, GraphQL surface, and intended architecture.
- `server/` is the most complete module today. It contains the working backend, persistence model, authentication, subscriptions, and most of the test coverage.
- `composeApp/` and `iosApp/` are still early scaffolding rather than a feature-complete product client.
- `shared/` exists as the intended shared Kotlin domain layer, but today it mostly contains starter/platform code instead of the business logic described in the spec.

## Read Next

- Domain rules and intended behavior: [`SPEC.md`](SPEC.md)
- Public-facing project overview: [`README.md`](README.md)
- Backend implementation state and server-specific guidance: [`server/AGENTS.md`](server/AGENTS.md)

## Working Guidance

- If a task depends on implemented behavior, trust the code and tests over the spec, then reconcile the docs if needed.
- For backend changes, treat the GraphQL SDL in `server/src/main/resources/graphql/` and the integration tests in `server/src/test/kotlin/` as part of the same change.
- The main architectural gap is still shared business logic: balance derivation, recurrence handling, and task completion rules are implemented in the server module instead of `shared/`.
- Before completing any task, run `./gradlew spotlessCheck`. If it fails, run `./gradlew spotlessApply` and then rerun `./gradlew spotlessCheck`.
