# IOU project overview
- Purpose: early-stage family coordination product for tracking children's balances, chores, rewards, invitations, and family-scoped money flows.
- Tech stack: Kotlin multi-module project. `server/` is Spring Boot + GraphQL + JPA + Liquibase with JWT auth. `composeApp/` is Compose Multiplatform client scaffolding. `shared/` holds canonical shared Kotlin domain/read DTO logic used by backend and clients. `iosApp/` is the iOS host app.
- Source of truth: trust implemented behavior and tests over `SPEC.md` when they differ, then reconcile docs if needed.
- Current implementation state: backend is the most complete area; frontend modules are still early scaffolding.
- Important architecture note: keep GraphQL-facing read DTOs in `shared/` when possible, and treat backend GraphQL SDL, server implementation, and integration tests as one change when behavior changes.
- Repo structure: root Gradle build with modules `:server`, `:shared`, and `:composeApp`.