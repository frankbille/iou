# IOU

## 💸 Family Money, Chores, and Rewards

IOU is an early-stage family coordination product for managing children's allowance-like balances, chores, and rewards in one place. The project is built around a simple problem: many families track earning, saving, and spending across paper notes, scattered messages, and one-off transfers. IOU aims to turn that into a shared system of record that feels understandable to both adults and children.

## 🏡 Home-Centered by Design

At its core, IOU treats a family as the boundary for everything that matters. Adults manage the household setup, children complete tasks and build balances, and money moves through named accounts that reflect how the family actually thinks about it, whether that is cash, a bank account, savings, or even a custom token system for younger kids.

The product is not just about recording chores. It is about making the rules around earning and reward more transparent. Tasks can be one-off or recurring, rewards can be paid immediately or only after approval, and balances are intended to come from a clear transaction history rather than guesswork.

## 👨‍👩‍👧‍👦 Who This Is For

### Parents and Guardians

The primary audience is adults who want a more structured way to run allowance and chore systems without losing the flexibility that real family life requires. IOU is aimed at households that want clearer expectations, cleaner tracking, and fewer informal calculations.

### Children

Children are also a core audience, even if the current codebase is still early. Over time, the product should help them understand what they can do, what they have earned, and how their balance changes.

### Builders Around the Product

This repository is also useful for product thinkers, designers, and engineers who need to reason carefully about family roles, approvals, balances, and reward policies before the experience is polished.

## 🚧 Current Stage

IOU is still in very early development, not even alpha. The domain and API surface are being shaped actively, and parts of the client experience still look like starter scaffolding rather than a finished product. What exists today is best understood as the foundation: the core family, task, invitation, account, and transaction model is being defined with enough rigor that future apps can grow on top of it.

## 🛠️ Tech Direction

Technically, this is a Kotlin-based multi-module project. The backend lives in `server/` and uses Spring Boot, GraphQL, JPA, Liquibase, and MySQL-oriented testing infrastructure. The client side is being set up as a Kotlin Multiplatform application with Compose Multiplatform in `composeApp/`, shared code in `shared/`, and an iOS host app in `iosApp/`.

The overall direction is a single product that can span platforms while sharing domain logic where it makes sense. At the moment, the backend domain model is further along than the user-facing app.

## Backend Authentication

The `server/` module now expects bearer JWT authentication on the GraphQL endpoint.

For local runs, provide a symmetric HMAC secret through `IOU_SECURITY_JWT_SECRET` (or the equivalent Spring property `iou.security.jwt-secret`).

The current baseline expects the JWT `sub` claim to contain a Rails-style GlobalID:

* `gid://iou/Parent/123`

The backend parses the subject to derive both the person type and the internal id. This is intentionally a resource-server baseline only. Token issuance, invite acceptance, and child authentication are still future work.

## 📘 More Detail

For the evolving domain and behavior rules, see [SPEC.md](./SPEC.md).
