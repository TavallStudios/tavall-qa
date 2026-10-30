# Tavall QA API Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-api` (`API`, secondary role `LIBRARY`)
> **Runtime owner:** `tavall-web-app`

## Current state

The API contracts model Task Board projects, tasks, scoped QA assignments, evidence review, acceptance, Human Verify, comments, audit entries, VQC contributions, authorization context, and sanitized public/staff projections. The public and private projections are separate types. Task-level Account/Minecraft identities and internal evidence remain within staff-scoped views.

The current candidate is QA PR #1; the workflow guard change rejects tasks only in REVIEW or DONE.

`QaSubjectContext` represents current Tavall Account facts: Account standing, linked Minecraft identity, organization/service binding, QA classes and tier, environment grants, explicit capabilities, and task-scoped assignment grants. A tier alone grants no capability. Technical QA remains subject to Tier III+ and active agreement requirements. This module contains contracts only; Web resolves current Account facts and the QA handler rechecks them at each operation.

## Phase 6 Notion 1:1 Task Mapping and WebMCP Task Views

- Added typed `NotionDatabaseTaskMapping` with configurable property/status mappings.
- Added `NotionTaskSnapshot`, `NotionTaskSyncState`, `NotionTaskOutboundChange`, `NotionTaskSyncAckRequest`, `NotionTaskSyncAckResult`, and `NotionTaskIngestResult`.
- Added agent-facing views `TaskBoardAgentTaskView` and `TaskBoardAgentTaskContextView` providing bounded task-specific context without a workspace dump.
- Added `TaskBoardAgentExecutionUpdateRequest` for agent execution/evidence updates.
- Added handler interfaces `ITaskBoardNotionSyncHandler`, `ITaskBoardAgentViewHandler`, and persistence contract `ITaskBoardNotionSyncDataHandler`.

## Validation

- Exact local source composition: `./gradlew --no-daemon test` from the `tavall-qa` repository.
- Latest local run: 28 tests passed across the Task Board test suite, including the isolated PostgreSQL integration test; 0 failures, 0 errors, 0 skips.
- `git diff --check` passes for the current candidate worktree.
- Exact current-source hosted Tavall CI, package validation, and merge acceptance are tracked on Issue #263 and the related PRs.

## Remaining gates

- Exact-source Tavall CI checks and repository review evidence.
- Web HTTP privacy/security and browser acceptance against the shared runtime.


## Execution continuation contracts — October 26, 2026

[QA #3](https://github.com/TavallStudios/tavall-qa/pull/3) adds typed execution source, bounded lease and immutable checkpoint data, eight explicit execution states, request contracts, and private Handler/DataHandler interfaces. Execution completion remains separate from task acceptance and Human Verify. API sources passed the Java 25 diagnostic and exact-source production compilation at `b5b03346419616aa527ce437d2c4b0871585da96` in run `73f90694-dfc7-4768-98b7-cd6ac2c6c3cf`. Current mapped/runtime acceptance is tracked in [SYSTEM progression](TAVALL_QA_SYSTEM_PROGRESSION.md#execution-checkpoint-implementation-candidate--october-26-2026); Storage payload publication, AI resumption and Web transport remain incomplete.
