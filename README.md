# Tavall QA

Authoritative backend task, progress, and QA verification plane for Tavall.

## Authority

`tavall-qa` is the canonical **mapped-JPA Task Board and QA verification authority**. It owns:
- Project lifecycle and audience definitions.
- Task graph, section mapping, priority, weight, and status.
- QA state (`NHV`, `REVIEW_REQUIRED`, `HUMAN_VERIFIED`), verifier identity, and timestamps.
- Action targets, discussion/suggestion/QA-note threads.
- Progression/VQC contribution tracking.
- PostgreSQL schema migrations (`schema/postgres/027_task_board.sql`).
- Public and staff query projections.

## Architecture

- **`tavall-qa-api`**: Typed domain records, enums, request contracts, view projections, and handler interfaces. (Primary module type: `API`; secondary role: `LIBRARY`).
- **`tavall-qa-core`**: Mapped JPA entities, `IPostgresDatabase` integration, mutation/query handlers, action target resolver, and SQL migrations. (Primary module type: `SYSTEM`; secondary role: `PROVIDER`).
- **`tavall-qa-test-suite`**: Unit and integration test coverage for queries, mutations, verification constraints, invalidation, and authorization. (Primary module type: `TEST_SUITE`).

## Principles

1. **NHV Default**: All tasks enter as `NHV` (Not Human Verified).
2. **`DONE != HUMAN_VERIFIED`**: A completed task requires authorized verification with linked Minecraft identity and valid evidence.
3. **Invalidation**: Material changes to task requirements, source, or completion status invalidate prior human verification.
4. **Single Source of Truth**: PostgreSQL mapped JPA authority; no duplicate writable progress database or legacy `ProgressTask` synchronization.
