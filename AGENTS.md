# AGENTS

Start here when working in this repository.

## Project State

- `SPEC.md` is the behavioral source of truth for the domain model, GraphQL surface, and intended architecture.
- `server/` is the most complete module today. It contains the working backend, persistence model, authentication, subscriptions, and most of the test coverage.
- `composeApp/` and `iosApp/` are still early scaffolding rather than a feature-complete product client.
- `shared/` is now the canonical shared Kotlin domain layer for read DTOs and balance derivation used by both the frontend and the backend.

## Read Next

- Domain rules and intended behavior: [`SPEC.md`](SPEC.md)
- Public-facing project overview: [`README.md`](README.md)
- Backend implementation state and server-specific guidance: [`server/AGENTS.md`](server/AGENTS.md)

## Working Guidance

- If a task depends on implemented behavior, trust the code and tests over the spec, then reconcile the docs if needed.
- For backend changes, treat the GraphQL SDL in `server/src/main/resources/graphql/` and the integration tests in `server/src/test/kotlin/` as part of the same change.
- Prefer keeping GraphQL read DTOs in `shared/` so the frontend and backend share the same schema-facing model.
- Balance derivation now lives in `shared/`; the main remaining architectural gap is recurrence and task-completion logic that still lives in `server/`.
- Before completing any task, run `./gradlew spotlessCheck`. If it fails, run `./gradlew spotlessApply` and then rerun `./gradlew spotlessCheck`.
