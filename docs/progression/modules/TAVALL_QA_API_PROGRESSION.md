# Tavall QA API Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-api` (`API`, secondary role `LIBRARY`)
> **Runtime owner:** `tavall-web-app`

## Current design-review status

| Boundary | State | Evidence / Remaining work |
| --- | --- | --- |
| Generic task-keyed review contracts and persistence implementation | MERGED | QA main `cd74c548476232f37584fbc12d2091d8ef0a934c`, promoting exact source `36af58d2e7200c80b2a06ca8cd47a1f317bb61ef` through [PR #24](https://github.com/TavallStudios/tavall-qa/pull/24). |
| Exact composed QA source validation | REQUIRED_ALL PASS | Request `9174c660-f796-4353-9b3f-85a8e42a858d`; COMPILATION, UNIT_TESTS, INTEGRATION and QUALITY all passed. |
| Web consumer delivery and preview browser | DEVELOPMENT/STAGING VERIFIED | Web source `ffb5e0acc89efd4722abd8b8bb98e2536e925bed`; registry generations 755/756; A/B/C at 1440px and 390px in [verified Web evidence](https://github.com/TavallStudios/tavall-web/pull/199#issuecomment-6857148017). Fixture preview acceptance does not validate winner persistence. |
| Live review persistence | BLOCKED | Development reports HTTP 500 / SQL `42P01`: `task_board_design_review` is absent. Migration `041_task_board_design_review.sql` needs an authorized schema operation; direct PostgreSQL access is forbidden to this worker and the installed Cloud database CLI exposes no migration command. |
| Production and owner/reviewer decision journey | UNVERIFIED | Real dashboard task `cf62acf6-e6b0-369d-8b4e-bca92a1aad29` exists in both boards, but candidates are not registered. Production migration/backup and artifact promotion remain unattempted; no owner-ready acceptance is claimed. |

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
| 2026-11-13 7:33 AM PST | `SOURCE_PROMOTED_RUNTIME_BLOCKED` | Generic design-review composition passed its exact root gate and merged to main. | [QA PR #24](https://github.com/TavallStudios/tavall-qa/pull/24), merge `cd74c548476232f37584fbc12d2091d8ef0a934c`; request `9174c660-f796-4353-9b3f-85a8e42a858d`. | Apply/verify migration 041 through the authorized migration owner; then register live candidates and verify the winner and Production journey. |


## Generic web design reviews — November 13, 2026

A task may own one `TaskBoardDesignReview` for any web surface, named by its persisted `surface` field. It stores 2–4 typed candidates; each candidate has a name, HTTPS preview URL, rationale, and optional `optionParameters` mapping option names to the values the candidate supports. The API does not identify a dashboard or constrain option names to a particular product. Product preview hosts enforce their own option allow-lists. If `optionParameters` is omitted from a request, the API normalizes it to an empty map.

The task-keyed aggregate is persisted by `tavall-qa-system` in `task_board_design_review`, with candidate payloads, surface, revision, registration attribution, and immutable winner attribution. Migration `041_task_board_design_review.sql` owns the table. Candidate registration and winner selection atomically update the review, task status, and audit entry using the existing Task Board transaction/row-lock path. Review reads enforce task management or staff visibility; winner selection additionally requires global QA administration or the assigned REVIEW_EVIDENCE authority and remains independent of task management alone.

`ITaskBoardQueryHandler.findOpenDesignReviews` supplies the role-filtered open review list for the QA review page. The Forge page renders live sandboxed preview frames, builds option controls from candidate metadata, supports single/compare and desktop/tablet/phone widths, and posts winner choices through the existing CSRF-protected Account boundary. The agent ledger client registers candidates with a surface and reads the persisted winner; it never selects a winner.

The earlier source-only validation note is superseded for source acceptance by the exact root gate above. Live target migration, persistent winner selection and Production browser acceptance remain incomplete. GitHub issue #23 retains the precise rollout blocker and current evidence.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-qa/docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | 2026-11-13 7:33 AM PST | QA PR #24 merge cd74c54 and exact-source request 9174c660. |
| Notion | `TEMPORARY_DRIFT` | No verified API-specific twin found | 2026-11-13 7:33 AM PST | Searches for Tavall QA API Progression and tavall-qa-api found the existing SYSTEM progression but no verified API-specific twin. The SYSTEM page receives the scoped acceptance checkpoint; no API 1:1 sync is claimed. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 7:33 AM PST | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | Same path | QA #24 merge cd74c54; request 9174c660 | Added current design-review source acceptance, runtime blocker and metadata footer. Earlier document history was not backfilled. |

</details>
