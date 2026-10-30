# Tavall QA SYSTEM Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-system` (`SYSTEM`, secondary role `PROVIDER`)
> **Runtime owner:** `tavall-web-app`

## Current state

The SYSTEM owns mapped-JPA Task Board persistence through Tavall Database's `IPostgresDatabase.entities()` API, checked-in PostgreSQL migrations `027` and `028`, query and mutation handlers, audience-first weighted progress projections, assignment/evidence workflows, append-only audit history, task revision invalidation, and idempotent accepted-task VQC attribution. Its DI wiring resolves the shared database lazily on first operation so the Web runtime can register its database before QA persistence is selected. It is composed into the shared Tavall Web application; it does not own a process or runtime.

Evidence review, task acceptance, and Human Verify are independent operations. Acceptance requires accepted evidence for the current task fingerprint and environment; it marks the task `DONE` while preserving `NHV`. VQC is awarded once per accepted eligible task to the evidence submitter. Rejection is available only for `REVIEW`/`DONE` tasks and changes the task revision so previous evidence is stale. Human Verify requires the separate authorization, valid evidence, active service context, and linked Minecraft identity.

## Phase 6 Notion 1:1 Task Mapping and WebMCP Task Views

- Implemented typed `NotionDatabaseTaskMapping` with configurable property and status mappings, with preconfigured canonical PvP and Kingdoms schemas.
- Implemented `TaskBoardNotionSyncHandler` with idempotent ingest, revision tracking via `last_edited_time`, conflict detection and resolution, archived/deleted task cancellation, and outbound sync export and acknowledgement.
- Implemented `NotionTaskSyncStateEntity` with checked-in migration `035_task_board_notion_sync.sql` and `TaskBoardNotionSyncDataHandler` featuring file-backed redundancy mirror via `tavall-filesystem` (`FilesystemAtomicOperations`) surviving restarts.
- Implemented `TaskBoardAgentViewHandler` delivering role-aware task discovery, bounded task-specific context without a workspace dump, and execution update capabilities through owner APIs.
- Comprehensive test suite in `TaskBoardNotionSyncTest`: 10 tests verifying create, update, conflict, archive, duplicate ingest, ack replay, unauthorized caller denial, agent views, custom database mapping, and restart-surviving file-backed state.

## Validation

- `./gradlew --no-daemon test` with `TAVALL_QA_TEST_POSTGRES_URL` set to a temporary PostgreSQL 17 container: 28 tests passed, 0 failures, 0 errors, 0 skips. The PostgreSQL test ran migrations 027/028 and exercised mapped persistence, concurrent acceptance retry/idempotency, rollback, stale-write rejection, Human Verify, and restart recovery.
- `git diff --check`: passes for the current candidate worktree.
- Web exact-source local tests: 411 tests passed, 0 failures, 0 errors, 9 unrelated Account PostgreSQL-gated skips.
- The shared `tavall-cloud-postgres` database was not modified.

## Remaining gates

- Pass exact-source Tavall CI and repository-owned review checks.
- Verify the migration against the actual shared DEVELOPMENT runtime database before deployment; no schema was applied in this source test.
- Merge and deploy only after the corresponding source, artifact, migration, and runtime gates pass.
