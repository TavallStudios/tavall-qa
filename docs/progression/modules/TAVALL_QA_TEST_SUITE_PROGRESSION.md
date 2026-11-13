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

## Generic web design reviews — November 13, 2026

A task may own one `TaskBoardDesignReview` for any web surface, named by its persisted `surface` field. It stores 2–4 typed candidates; each candidate has a name, HTTPS preview URL, rationale, and optional `optionParameters` mapping option names to the values the candidate supports. The API does not identify a dashboard or constrain option names to a particular product. Product preview hosts enforce their own option allow-lists.

The task-keyed aggregate is persisted by `tavall-qa-system` in `task_board_design_review`, with candidate payloads, surface, revision, registration attribution, and immutable winner attribution. Migration `041_task_board_design_review.sql` owns the table. Candidate registration and winner selection atomically update the review, task status, and audit entry using the existing Task Board transaction/row-lock path. Review reads enforce task management or staff visibility; winner selection additionally requires global QA administration or the assigned REVIEW_EVIDENCE authority and remains independent of task management alone.

`ITaskBoardQueryHandler.findOpenDesignReviews` supplies the role-filtered open review list for the QA review page. The Forge page renders live sandboxed preview frames, builds option controls from candidate metadata, supports single/compare and desktop/tablet/phone widths, and posts winner choices through the existing CSRF-protected Account boundary. The agent ledger client registers candidates with a surface and reads the persisted winner; it never selects a winner.

As of this source update, focused local API/handler, persistence simulation, QA web page, host controller, browser, composed staging and deployment validation are still pending. No runtime or public UI acceptance is inferred from source. Notion synchronization is unavailable in this provider harness (`notion.fetch` returned Unknown tool); the GitHub issue remains the work authority.
