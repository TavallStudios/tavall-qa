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

## Remaining gates

- Pass exact-source Tavall CI and repository-owned review checks.
- Verify the migration against the actual shared DEVELOPMENT runtime database before deployment; no schema was applied in this source test.
- Merge and deploy only after the corresponding source, artifact, migration, and runtime gates pass.

<details>
<summary>Documentation Update State</summary>

### Current Locations
| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PR_DRAFT` | `TavallStudios/tavall-qa/docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md` | 2026-10-25 2:17 AM PDT | Docs-only branch `docs/qa-board-examples-20261025`; linked draft PR |
| Notion | `1:1_DRAFT_PR` | [Notion twin](https://app.notion.com/p/40438458ddfd812fb5f1d73a310307d1) | 2026-10-25 2:17 AM PDT | Twin to be created in this pass |

### Update History
| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-10-25 2:17 AM PDT | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md` | Same file | Branch `docs/qa-board-examples-20261025` | Added verified source references, current design scope and footer. |

</details>
