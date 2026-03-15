# Server AGENTS

`server/` is the most complete part of the repository. It is a Spring Boot GraphQL API backed by JPA and Liquibase, and it is the first place to look for behavior that already exists in code.

## What Exists

- GraphQL SDL lives in `src/main/resources/graphql/`.
- Runtime code is organized by domain package under `src/main/kotlin/dk/frankbille/iou/` (`family`, `invitation`, `moneyaccount`, `taskcategory`, `task`, `transaction`, `events`, `security`, and related support packages).
- Controllers expose queries, mutations, schema mappings, and subscriptions. Services hold most business rules. Entities and repositories back the persistence layer.
- `/graphql` requires JWT bearer auth. The JWT subject is parsed as a GlobalID such as `gid://iou/Parent/123` or `gid://iou/Child/456`, and family-level access is enforced through Spring Security plus the `FamilyScopeCheck` mechanism.
- The current server covers viewer resolution for both parents and children, family and membership management, invitation expiry, money accounts, task categories, one-off and recurring task flows, manual ledger mutations, reward transactions, and `familyEvents` subscriptions.
- Realtime event delivery is currently single-instance only through the in-memory distributor under `src/main/kotlin/dk/frankbille/iou/events/singleinstance/`.

## Where Behavior Lives

- Family and membership behavior: `src/main/kotlin/dk/frankbille/iou/family/`
- Invitation lifecycle: `src/main/kotlin/dk/frankbille/iou/invitation/`
- Task creation, completion, approval, and recurrence handling: `src/main/kotlin/dk/frankbille/iou/task/TaskService.kt`
- Ledger writes and server-side transaction orchestration: `src/main/kotlin/dk/frankbille/iou/transaction/TransactionService.kt`
- Shared read DTOs and balance derivation: `../shared/src/commonMain/kotlin/dk/frankbille/iou/`
- Security, viewer resolution, and family scoping: `src/main/kotlin/dk/frankbille/iou/security/`
- Event publication and subscriptions: `src/main/kotlin/dk/frankbille/iou/events/`

## Tests

- `src/test/kotlin/` is mostly integration coverage organized by domain.
- `src/test/kotlin/dk/frankbille/iou/test/GraphQlControllerIntegrationTest.kt` is the shared GraphQL integration harness, including database cleanup and authenticated parent/child test clients.
- Run `./gradlew :server:test` after changing server behavior.

## Known Gaps

- `SPEC.md` expects more client/server business logic in `shared/`. Balance calculation and read DTOs are shared now, but recurrence logic and task completion rules still live in server services.
- Invitation acceptance and identity-linking flows are still intentionally deferred; the implemented invitation lifecycle currently covers creation, viewing, revocation, and expiry.
- When you change backend behavior, update the SDL, service/controller code, integration tests, and shared DTOs or shared logic together when they are schema-relevant.
