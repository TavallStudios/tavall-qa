# Tavall QA SYSTEM Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-system` (`SYSTEM`, secondary role `PROVIDER`)
> **Runtime owner:** `tavall-web-app`

## Current state

The SYSTEM owns mapped-JPA Task Board persistence through Tavall Database's `IPostgresDatabase.entities()` API, checked-in PostgreSQL migrations `027` and `028`, query and mutation handlers, audience-first weighted progress projections, assignment/evidence workflows, append-only audit history, task revision invalidation, and idempotent accepted-task VQC attribution. Its DI wiring resolves the shared database lazily on first operation so the Web runtime can register its database before QA persistence is selected. It is composed into the shared Tavall Web application; it does not own a process or runtime.

Evidence review, task acceptance, and Human Verify are independent operations. Acceptance requires accepted evidence for the current task fingerprint and environment; it marks the task `DONE` while preserving `NHV`. VQC is awarded once per accepted eligible task to the evidence submitter. Rejection is available only for `REVIEW`/`DONE` tasks and changes the task revision so previous evidence is stale. Human Verify requires the separate authorization, valid evidence, active service context, and linked Minecraft identity.

## Validation

- `./gradlew --no-daemon test` with `TAVALL_QA_TEST_POSTGRES_URL` set to a temporary PostgreSQL 17 container: 28 tests passed, 0 failures, 0 errors, 0 skips. The PostgreSQL test ran migrations 027/028 and exercised mapped persistence, concurrent acceptance retry/idempotency, rollback, stale-write rejection, Human Verify, and restart recovery.
- `git diff --check`: passes for the current candidate worktree.
- Web exact-source local tests: 411 tests passed, 0 failures, 0 errors, 9 unrelated Account PostgreSQL-gated skips.
- The shared `tavall-cloud-postgres` database was not modified.

## October 25 documentation-only source checkpoint

**Audited main:** `TavallStudios/tavall-qa@85779a99e8ab338c0366e3958d2a58da3023747b`. Source-level examples for the existing [TaskBoardQueryHandler](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/handler/TaskBoardQueryHandler.java), [TaskBoardTaskEntity](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/persistence/entity/TaskBoardTaskEntity.java), and [TaskBoardMutationHandler](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/handler/TaskBoardMutationHandler.java) were inspected for the accompanying Technical update. **No test, migration, live QA Web browser or Notion/GitHub sync run was performed in this documentation-only pass**. Previous test receipts in the Validation section remain historical evidence tied to their original source context, not a fresh current-head acceptance.

The newly approved Notion/GitHub 1:1 task projection, WebMCP task-view migration, and cross-provider checkpoints are `DESIGNED / NOT_VALIDATED` features. Do not mark them complete until their owner APIs, exact build, integration receipts and shared runtime verification exist. Parent aggregate PROGRESSION must link this module record rather than copying each task/revision event.

## Execution checkpoint implementation candidate — October 26, 2026

[QA #3](https://github.com/TavallStudios/tavall-qa/pull/3) adds actual mapped lease/checkpoint entities, an atomic checkpoint DataHandler, current Account/DEV authorization and generated Tavall DI composition. The existing task lock is shared with checkpoint mutations. Migration 034 and private native PostgreSQL integration scenarios are committed. Prior validation sections preserve QA #1 history and do not validate this new source.

| Gate | Exact evidence | Current result / limit |
| --- | --- | --- |
| API compiler diagnostic | All 72 API Java sources, Java 25 | PASS; local API-only diagnostic |
| Production compilation | `b5b03346419616aa527ce437d2c4b0871585da96`, run `73f90694-dfc7-4768-98b7-cd6ac2c6c3cf`, evidence SHA-256 `2b6d9c0c385f55b481cc11237f21d5f8c1499b30c63fd23106f3e0046a88aedb` | PASS for that exact source |
| Current production/test compilation | `fd44ada8e99ff7349f70386c6a4624131ce52ce4`, run `aa3ee6e9-772f-4cab-8c22-d2cb8bf4ed01`, evidence SHA-256 `397a421f8f8760a8a32e9cf78cd779d1904b6f62fc84a17e6fd345eceb6abf81` | PASS |
| Required native PostgreSQL integration | Same source, run `8155a399-add7-4651-831f-ea4e7e4e341e`, evidence SHA-256 `6fae31c9a4803ed5d15806a126de4bf2da3ba5cb56420e215fb549e1ab7cdd83` | PASS; private native PostgreSQL, actual Database mappings and scoped DI |
| Unit profile | Same source, run `abdaa3c9-f273-453f-b2f8-3611b3e21589`, evidence SHA-256 `43efaef0c4666a4c0f90fe3b37135f183ee4caea99f9b734da46f6bd4dbf62da` | PASS |
| Real payload, source guards and provider resume | No current runtime receipt | Incomplete |
| Shared Web routes, migration, QA UI and deployment | No current acceptance | Incomplete; existing runtime and product design retained |

## Typed payload and source-guarded resumption

Source `1fe4770fb86439c881c6ec5cae2d2c3bafa4cd3b` adds actual QA-owned typed payload persistence, canonical JSON/digest mapping, bounded patch context, separate authorized body retrieval and immutable envelope/body links. It passed compilation in run `e401039f-7bfa-4683-92ab-0e36e4073434`, required native PostgreSQL integration in `47418912-3e65-4ab7-a8ea-c741895cd6cc`, and unit tests in `240ae975-cad7-4dac-9c39-779f14e54c18`. Evidence SHA-256 values are respectively `900bd3b9208a7888c77c6eda9a3b91b12fdc45057b695cf4aa3e53b601a71818`, `40fa4a7557ccda8941edcf7ff1c314a888f7e4244739006d8c5f5f07fcc6f58f`, and `deea72b286ab65b05110fc2d00fb3435c62c7410eebfdb6b88fdf19edfb4ff61`.

Subsequent source `6bed27ee0cd7369f88c29cc0d2cce518851a6edd` adds atomic latest-checkpoint/source/diff guards before resume lease acquisition and a stable version-one serialization fixture. Compilation request `34cbeab2-5193-4c3f-8073-8f6473cee925` is running. Its new tests are not yet accepted; real AI host workspace verification, current capability equivalence, provider invocation, shared Web endpoints and migration application remain incomplete.

## Remaining gates

- Pass exact-source Tavall CI and repository-owned review checks.
- Verify the migration against the actual shared DEVELOPMENT runtime database before deployment; no schema was applied in this source test.
- Merge and deploy only after the corresponding source, artifact, migration, and runtime gates pass.

<details>
<summary>Documentation Update State</summary>

### Current Locations
| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PR_DRAFT` | `TavallStudios/tavall-qa/docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md` | 2026-10-25 2:17 AM PDT | Original docs PR #2; checkpoint implementation PR #3 |
| Notion | `1:1_DRAFT_PR` | [Notion twin](https://app.notion.com/p/40438458ddfd812fb5f1d73a310307d1) | 2026-10-25 2:17 AM PDT | Existing typed page retained; checkpoint content update pending |

### Update History
| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-10-25 2:17 AM PDT | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md` | Same file | Branch `docs/qa-board-examples-20261025` | Added verified source references, current design scope and footer. |

| 2026-10-26 10:59 AM PDT | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md` | Same logical document from PR #2 | PR #3, source `fd44ada` and exact CI receipts | Documented real DI/Database checkpoint implementation; kept runtime, payload and provider-continuation gates separate. |
</details>
