# Tavall QA API Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-qa-api` (`API`, secondary role `LIBRARY`)
> **Runtime owner:** `tavall-web-app`

## Current design-review status

| Boundary | State | Evidence / Remaining work |
| --- | --- | --- |
| Generic task-keyed review contracts and persistence implementation | MERGED | QA main `cd74c548476232f37584fbc12d2091d8ef0a934c`, exact source `36af58d2e7200c80b2a06ca8cd47a1f317bb61ef`, [PR #24](https://github.com/TavallStudios/tavall-qa/pull/24). |
| Exact composed QA source validation | REQUIRED_ALL PASS | Request `9174c660-f796-4353-9b3f-85a8e42a858d`; historical source gate, separate from current runtime acceptance. |
| A/B/C preview presentation | VERIFIED | Dashboard PR #27 evidence contains blank-profile Chromium captures at 1440×1000 and 390×844; all three fixture URLs returned HTTP 200 and phone captures show no horizontal clipping. Static sample content only. |
| Authenticated task page and review read | HUMAN_REQUIRED | The public task page still redirects anonymously to sign-in (301/302). The single remaining owner action is an authenticated browser sign-in at `http://localhost:19081` and opening review task `cf62acf6-e6b0-369d-8b4e-bca92a1aad29`; no candidate/winner write is claimed by this lane. |
| Candidate registration / persistence contract | VERIFIED (DB layer) | Migration 041 applied out-of-band to DEVELOPMENT `novus_web`; `task_board_design_review` verified with 10 columns, CHECK constraints `task_board_design_candidates_check`, `task_board_design_decision_check`, `task_board_design_review_revision_check`, and DML grants to app role `novus`. A candidate A/B/C registration → winner C round-trip committed then rolled back (zero residue). Contract coverage in [PR #31](https://github.com/TavallStudios/tavall-qa/pull/31): 8/8 unit and 4/4 real PG14 integration tests, exact head `499f8ee4f1b60338a17963012a179370e2717a7a` REQUIRED_ALL PASS (request `44fcc365-08ab-49e7-9d47-df7f88ce7869`). The historical Nov 13 `42P01` and masked HTTP 500 arose before the migration existed and are superseded at the database layer; the authenticated browser journey remains the owner step above. |
| Shared runtime and deployability | OWNER_STEP | QA is composed into the shared `tavall-web` runtime; there is no independent QA service. Deploys do not apply DDL (`generateSchema` false). The migration was applied through the owner-documented out-of-band path, not by an app boot or a deploy. |
| Production and owner/reviewer decision journey | VERIFIED (DB layer) / HUMAN_REQUIRED (browser) | Migration 041 applied to PRODUCTION `tavall_web_production` (backup `/srv/storage-0/backups/novus_web/tavall_web_production_pre041_20261115T130633Z.dump`, readable via `pg_restore -l`); table, 10 columns, three CHECK constraints and DML grants to `tavall_web_production` verified; candidate A/B/C → winner C round-trip committed then rolled back. Studio/C remains the approved default; no restyle. The authenticated winner-selection browser click is the single remaining owner action. |

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

| `2026-11-15 13:08 UTC` | `MIGRATION_APPLIED_PERSISTENCE_VERIFIED` | Applied migration 041 out-of-band to DEVELOPMENT and PRODUCTION; verified table, columns, CHECK constraints, grants and a rolled-back candidate/winner round-trip. | Backups `novus_web_pre041_20261115T125723Z.dump` and `tavall_web_production_pre041_20261115T130633Z.dump`; [PR #31](https://github.com/TavallStudios/tavall-qa/pull/31) merged at `c4fb7844cc0d139ed9506b1ecab46f49c81d81bb`; exact-head REQUIRED_ALL `44fcc365-08ab-49e7-9d47-df7f88ce7869`. | Database layer verified; the single remaining owner action is an authenticated browser sign-in and winner selection. An anonymous public read still redirects (301/302), so no live authenticated HTTP read is claimed by this lane. |
| `2026-11-15 00:44 UTC` | `PREVIEW_VERIFIED_RUNTIME_BLOCKED` | Verified A/B/C static previews at desktop and 390px and rechecked the shared QA API/runtime. | Dashboard PR #27 evidence; public QA page 302, typed review read HTTP 500, registration preflight stopped before POST; Cloud inventory generation 769 DEGRADED, no managed releases. | No candidate/winner write or deploy; authorized Web runtime/migration owner must restore a deployable shared runtime and apply/verify migration 041. |
| `2026-11-04 21:01 UTC` | `MERGED_PRODUCTION` | Add QA external reference reconciliation. | [TavallStudios/tavall-qa#8](https://github.com/TavallStudios/tavall-qa/pull/8) merged as `e6c89ea`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 03:44 UTC` | `MERGED_PRODUCTION` | Test: Convert tavall-qa-api tests to house style. | [TavallStudios/tavall-qa#11](https://github.com/TavallStudios/tavall-qa/pull/11) merged as `7adeac1`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| `2026-11-05 05:19 UTC` | `MERGED_PRODUCTION` | Convert tavall-qa test-suite tests to house style. | [TavallStudios/tavall-qa#13](https://github.com/TavallStudios/tavall-qa/pull/13) merged as `ff0a8db`. | Exact-source CI request id and DEVELOPMENT deploy generation are not retrievable from merged history; validation and deployment state `unverified`. |
| 2026-11-13 7:33 AM PST | `SOURCE_PROMOTED_RUNTIME_BLOCKED` | Generic design-review composition passed its exact root gate and merged to main. | [QA PR #24](https://github.com/TavallStudios/tavall-qa/pull/24), merge `cd74c548476232f37584fbc12d2091d8ef0a934c`; request `9174c660-f796-4353-9b3f-85a8e42a858d`. | Apply/verify migration 041 through the authorized migration owner; then register live candidates and verify the winner and Production journey. |


## Generic web design reviews — November 13, 2026

A task may own one `TaskBoardDesignReview` for any web surface, named by its persisted `surface` field. It stores 2–4 typed candidates; each candidate has a name, HTTPS preview URL, rationale, and optional `optionParameters` mapping option names to the values the candidate supports. The API does not identify a dashboard or constrain option names to a particular product. Product preview hosts enforce their own option allow-lists. If `optionParameters` is omitted from a request, the API normalizes it to an empty map.

The task-keyed aggregate is persisted by `tavall-qa-system` in `task_board_design_review`, with candidate payloads, surface, revision, registration attribution, and immutable winner attribution. Migration `041_task_board_design_review.sql` owns the table. Candidate registration and winner selection atomically update the review, task status, and audit entry using the existing Task Board transaction/row-lock path. Review reads enforce task management or staff visibility; winner selection additionally requires global QA administration or the assigned REVIEW_EVIDENCE authority and remains independent of task management alone.

`ITaskBoardQueryHandler.findOpenDesignReviews` supplies the role-filtered open review list for the QA review page. The Forge page renders live sandboxed preview frames, builds option controls from candidate metadata, supports single/compare and desktop/tablet/phone widths, and posts winner choices through the existing CSRF-protected Account boundary. The agent ledger client registers candidates with a surface and reads the persisted winner; it never selects a winner.

The earlier source-only validation note is superseded for source acceptance by the exact root gate above. Migration 041 is now applied and its persistence contract verified in both DEVELOPMENT and PRODUCTION (see status table and timeline above); the remaining step is the single authenticated browser sign-in and winner selection by the owner. GitHub issue #23 retains the rollout evidence and the human-required action.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-qa/docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | 2026-11-15 15:20 UTC | Migration 041 applied/verified in DEV+PROD; PR #31 contract tests REQUIRED_ALL PASS; single owner browser sign-in remains. |
| Notion | `TEMPORARY_DRIFT` | No verified API-specific twin found | 2026-11-13 7:33 AM PST | Searches for Tavall QA API Progression and tavall-qa-api found the existing SYSTEM progression but no verified API-specific twin. The SYSTEM page receives the scoped acceptance checkpoint; no API 1:1 sync is claimed. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 7:33 AM PST | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | Same path | QA #24 merge cd74c54; request 9174c660 | Added current design-review source acceptance, runtime blocker and metadata footer. Earlier document history was not backfilled. |
| 2026-11-15 00:44 UTC | GitHub | `UPDATED_PENDING_NOTION_SYNC` | `docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | Same path | Dashboard PR #27 screenshot evidence; public API observations; Cloud inventory | Reconciled current runtime, page, read and registration-preflight outcomes; no candidate write claimed. |
| 2026-11-15 15:20 UTC | GitHub | `UPDATED` | `docs/progression/modules/TAVALL_QA_API_PROGRESSION.md` | Same path | Migration 041 applied/verified in DEV+PROD; PR #31 merged and gated | Replaced stale BLOCKED registration/production rows with verified DB-layer status and a single HUMAN_REQUIRED browser step; earlier history retained. |

</details>
