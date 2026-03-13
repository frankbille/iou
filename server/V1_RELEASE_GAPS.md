# Server V1 Release Gaps

This file tracks implementation gaps found by comparing the `server` module against `SPEC.md` and the GraphQL schema files under `server/src/main/resources/graphql/`.

Review status:
- Reviewed query, mutation, schema-mapping, service, security, and invitation/task/transaction code paths.
- Ran `./gradlew :server:test` on 2026-03-13. The current test suite passes.

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

## Remaining Gaps

### 1. Child-authenticated viewer flow is not implemented

Why this is a gap:
- The schema models `Viewer.person` as `Person!`, and `Person` can be either `Parent` or `Child`.
- `SPEC.md` also describes the authenticated viewer as a parent or child identity.
- The security and viewer code paths are parent-only today.

What is missing:
- Authentication support for child principals
- A viewer abstraction that can resolve either a parent or a child
- Authorization rules for child access
- Tests for child-authenticated GraphQL access

Current state:
- JWT conversion only accepts `Parent` global IDs
- `CurrentViewer` only exposes `parentId()`
- `viewer.person` is resolved through `ParentService`

Suggested TODO:
- Decide whether child sessions are in scope for V1. If yes, implement the full child-authenticated viewer path.

### 2. Parent invitation expiry lifecycle is only modeled, not operational

Why this is a gap:
- The schema exposes `expiresAt` and `EXPIRED`.
- `SPEC.md` describes expiring invitations as part of the invitation state model.
- The current service creates pending invitations and allows revocation, but does not assign an expiry or transition invitations to `EXPIRED`.

What is missing:
- A policy for setting `expiresAt`
- Logic to mark invitations as expired
- Tests for expiry behavior and expiry-aware reads/mutations

Current state:
- `inviteParentToFamily` sets `email`, `createdAt`, and `invitationNonce`
- `revokeParentInvitation` transitions only from `PENDING` to `REVOKED`

Suggested TODO:
- Either implement invitation expiry for V1 or explicitly narrow the V1 invitation lifecycle to `PENDING` and `REVOKED`.

### 3. Shared business logic is not implemented in the shared module

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

1. Child-authenticated viewer flow
2. Parent invitation expiry
3. Shared client/server business logic
