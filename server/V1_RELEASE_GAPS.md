# Server V1 Release Gaps

This file tracks implementation gaps found by comparing the `server` module against `SPEC.md` and the GraphQL schema files under `server/src/main/resources/graphql/`.

Review status:
- Reviewed query, mutation, schema-mapping, service, security, and invitation/task/transaction code paths.
- Ran `./gradlew :server:test` on 2026-03-14. The current test suite passes.

## Completed

### Family event subscriptions

Status:
- Done on 2026-03-13

Implemented:
- Added a `familyEvents` GraphQL subscription handler
- Added concrete event payload models for the `FamilyEvent` union
- Added an in-memory family-scoped event publisher
- Wired mutation controllers to publish changed, deleted, completion, and transaction events
- Added integration coverage for changed events, deleted events, transaction events, and family scoping

Notes:
- The implementation now covers the family-level realtime sync path that was previously only declared in the schema/spec.
- The full server test suite passes with the subscription work included.

### Child-authenticated viewer flow

Status:
- Done on 2026-03-14

Implemented:
- Added authentication support for child principals
- Added a viewer path that resolves either a parent or a child identity
- Added authorization rules for child access
- Added tests for child-authenticated GraphQL access

Notes:
- The authenticated viewer path now matches the schema and spec expectation that `viewer.person` can resolve to either `Parent` or `Child`.
- The remaining V1 server work now moves to shared client/server business logic.

### Parent invitation expiry lifecycle

Status:
- Done on 2026-03-14

Implemented:
- Added a 7-day expiry policy for new parent invitations
- Added lazy expiry reconciliation so stale pending invitations transition to `EXPIRED` during reads and revocation attempts
- Added integration coverage for expiry assignment, expiry-aware reads, and expiry-aware revocation behavior

Notes:
- Invitation lifecycle behavior now matches the schema and spec expectation that invitations expose `expiresAt` and can transition to `EXPIRED`.
- Parent invitation GraphQL date-time fields now serialize correctly when `expiresAt` is present.
- The next V1 server gap is shared client/server business logic.

## Remaining Gaps

### 1. Shared business logic is not implemented in the shared module

Why this is a gap:
- `SPEC.md` says balance derivation, recurrence calculations, and task completion rules should live in shared Kotlin modules used by both client and server.
- In the current codebase, those rules live in server services.

What is missing:
- Shared implementations for balance derivation
- Shared implementations for recurrence resolution
- Shared implementations for task completion rules
- Tests proving server and shared logic stay aligned

Current state:
- Balance derivation is implemented in `TransactionService`
- Recurrence and completion date resolution are implemented in `TaskService`
- The `shared` module is present but does not contain this domain logic yet

Suggested TODO:
- Treat this as an architectural V1 gap if the client is expected to compute projections locally from front-loaded data.

## Explicitly Not Counted As Gaps

These were not included above because the spec explicitly leaves them open or deferred:
- Parent invitation acceptance flow details
- Existing-child linking flow for `addChildToFamily`
- Other future identity-proofing details around invite acceptance

## Recommended Order

1. Shared client/server business logic
