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


## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| `2026-11-04 21:01 UTC` | `MERGED_PRODUCTION` | Add QA external reference reconciliation. | [TavallStudios/tavall-qa#8](https://github.com/TavallStudios/tavall-qa/pull/8) merged as `e6c89ea`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 03:44 UTC` | `MERGED_PRODUCTION` | Test: Convert tavall-qa-api tests to house style. | [TavallStudios/tavall-qa#11](https://github.com/TavallStudios/tavall-qa/pull/11) merged as `7adeac1`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 05:19 UTC` | `MERGED_PRODUCTION` | Convert tavall-qa test-suite tests to house style. | [TavallStudios/tavall-qa#13](https://github.com/TavallStudios/tavall-qa/pull/13) merged as `ff0a8db`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |

## Generic web design reviews — November 13, 2026

A task may own one `TaskBoardDesignReview` for any web surface, named by its persisted `surface` field. It stores 2–4 typed candidates; each candidate has a name, HTTPS preview URL, rationale, and optional `optionParameters` mapping option names to the values the candidate supports. The API does not identify a dashboard or constrain option names to a particular product. Product preview hosts enforce their own option allow-lists. If `optionParameters` is omitted from a request, the API normalizes it to an empty map.

The task-keyed aggregate is persisted by `tavall-qa-system` in `task_board_design_review`, with candidate payloads, surface, revision, registration attribution, and immutable winner attribution. Migration `041_task_board_design_review.sql` owns the table. Candidate registration and winner selection atomically update the review, task status, and audit entry using the existing Task Board transaction/row-lock path. Review reads enforce task management or staff visibility; winner selection additionally requires global QA administration or the assigned REVIEW_EVIDENCE authority and remains independent of task management alone.

`ITaskBoardQueryHandler.findOpenDesignReviews` supplies the role-filtered open review list for the QA review page. The Forge page renders live sandboxed preview frames, builds option controls from candidate metadata, supports single/compare and desktop/tablet/phone widths, and posts winner choices through the existing CSRF-protected Account boundary. The agent ledger client registers candidates with a surface and reads the persisted winner; it never selects a winner.

As of this source update, focused local API/handler, persistence simulation, QA web page, host controller, browser, composed staging and deployment validation are still pending. No runtime or public UI acceptance is inferred from source. Notion synchronization is unavailable in this provider harness (`notion.fetch` returned Unknown tool); the GitHub issue remains the work authority.
