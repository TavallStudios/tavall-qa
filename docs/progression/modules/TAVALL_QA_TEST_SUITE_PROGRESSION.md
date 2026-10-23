# Tavall QA Test Suite Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-test-suite` (`TEST_SUITE`)
> **Runtime owner:** None

## Current state

The module tests authorization intersections, public/private query filtering, mutation boundaries, NHV defaults, evidence review, task acceptance, one-per-task VQC, rejection invalidation, Human Verify prerequisites, audit events, and schema resource ownership.

## Latest local result

- Command: `TAVALL_QA_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:<isolated-port>/<disposable-db> ./gradlew --no-daemon test` from `tavall-qa`.
- 28 tests passed; 0 failures, 0 errors, 0 skips.
- The temporary PostgreSQL 17 database exercised real checked-in migrations, Tavall Database JPA mappings, transactional rollback, concurrent acceptance retry/idempotency, stale-write rejection, Human Verify, and restart recovery.
- The temporary database container was stopped after acceptance; the shared `tavall-cloud-postgres` service was not modified.
