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


## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| `2026-11-04 21:01 UTC` | `MERGED_PRODUCTION` | Add QA external reference reconciliation. | [TavallStudios/tavall-qa#8](https://github.com/TavallStudios/tavall-qa/pull/8) merged as `e6c89ea`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 04:40 UTC` | `MERGED_PRODUCTION` | Tests: convert tavall-qa-system to Tavall house style. | [TavallStudios/tavall-qa#12](https://github.com/TavallStudios/tavall-qa/pull/12) merged as `0c88cc2`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 05:19 UTC` | `MERGED_PRODUCTION` | Convert tavall-qa test-suite tests to house style. | [TavallStudios/tavall-qa#13](https://github.com/TavallStudios/tavall-qa/pull/13) merged as `ff0a8db`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 17:10 UTC` | `MERGED_PRODUCTION` | Add: seed admin-only tavall-engineering Task Board project. | [TavallStudios/tavall-qa#14](https://github.com/TavallStudios/tavall-qa/pull/14) merged as `68f03ee`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |