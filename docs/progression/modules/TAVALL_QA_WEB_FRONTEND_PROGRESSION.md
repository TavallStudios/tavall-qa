# Tavall Web QA Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-web-qa` (`API`, secondary role `LIBRARY`)
> **Runtime owner:** `tavall-web-app`

## Current implementation

The presentation preserves the accepted Direction C/A/C design source and uses the existing Forge primitives. It now renders request-bound public summaries, assigned work, evidence, authorized staff forms, task acceptance, Human Verify, task editing, and threaded discussion. Form submissions include CSRF and explicit organization context; the controller and QA API recheck all authority. Project task groups emit typed `data-tavall-qa-*` hooks for the existing three-visible/four-per-click disclosure. Task expansion, staff soft-select, Focus Workspace modes, and URL-hash restoration compile from `src/main/ts/qa-board.ts` and run through the typed Forge page-load plan on each QA page. The message route remains an authorization handoff to the shared Web message editor; this module owns no editor or message persistence.

WebQA PR #2 merged at `5943bbd7ba8b576fb50c64de3b1b6537d30f77a` and consumes QA API PR #1 at exact source head `516a77c21d06c6ac9c98511edcadd5a9fa1c213d`. The Tavall Web runtime integration PR #76 is merged; its follow-up uses the existing message editor owner and removes the unused static QA editor.

## Local validation

- Command: `./gradlew --no-daemon clean test` with local exact-source Web and QA composites.
- Result: 6 tests passed; 0 failures, 0 errors, 0 skips, including current accepted-evidence acceptance controls and stale-evidence suppression.
- Tavall TypeScript compilation and resource packaging passed as part of the same build; page tests assert that the QA initializer is included in the main board and staff workspace plans.
- `git diff --check` passes for the current candidate worktree.
- The Web application consumer test run passed 411 tests with 9 PostgreSQL-gated skips.

## Shared-runtime browser acceptance — 2026-10-23

The Web consumer candidate `TavallStudios/tavall-web@119562a080ecf8772a726db7d188872a455ee253` uses this presentation source at `241cc4e14b3ad3483bce165013ec79e04d011a99` with QA authority `516a77c21d06c6ac9c98511edcadd5a9fa1c213d` and Dashboard catalog `61246e952c7454b64bdca2bbddb3dc3e3c144f03`. The integrated Web source passed `check`, standalone verification, architecture tests, and the following local Chromium checks against a disposable PostgreSQL 17 fixture:

- Public C project index, A project/task pages, and C Focus Workspace were checked at 1440px and 390px. Workspace URL-hash restoration, keyboard Edit, Thread focus, and 3 → 7 → 8 progressive task expansion passed.
- Public task HTML retained NHV and did not include the synthetic internal description or synthetic Account/Minecraft identifiers. All checked pages had no horizontal overflow or browser console errors.
- The existing shared Web Message Editor rendered the desktop A index/form/preview layout and mobile C Preview/Edit/Messages modes. Message filtering, keyboard Edit, and Save & Return to the same task, organization context, and workspace hash passed.

Sanitized public and Message Editor screenshots and the exact result report are committed in the Web App [QA Board evidence](https://github.com/TavallStudios/tavall-web/tree/working/qa-message-editor-completion-20261023/docs/progression/evidence/2026-10-23-qa-board-completion). The Workspace screenshot was not published because the local fixture showed a synthetic Account identifier. These are local artifact/browser checks, not hosted TCI, remote Account SSO, or runtime acceptance.

## Remaining gates

- Merge WebQA PR #3 with its exact-source Web consumer follow-up; the accepted layouts and source ownership do not require another design round.
- Run current exact-head Tavall CI, deliver an immutable candidate through the managed deployment path, and verify the QA host on STAGING. The current live DEVELOPMENT baseline serves the host, but the new editor follow-up is not deployed.
- An authenticated QA Account with the explicit task capabilities must verify revocation and submit real public work through the QA API. The live public index currently has no publicly visible tasks; the private task count was not queried.
- Connect Cache/Event, CMS, roadmap, Discord, and public-proof subscribers only after their current owners expose typed contracts and the privacy/idempotency checks pass.


## Generic live design review — November 13, 2026

The QA presentation reads the review surface and candidate option maps from the typed Task Board projection. It renders live sandboxed frames, exposes only candidate-declared option values, and provides a cross-task list of open reviews visible to the current staff subject. Winner forms remain server-authorized and CSRF-protected. The implementation has no dashboard product dependency; dashboard A/B/C is the first registered use. Local page tests, exact hosted checks, browser screenshots and runtime acceptance remain pending for the current source.

## Current Status

| Area | State | Evidence |
| --- | --- | --- |
| Page test house style | `LOCAL_VALIDATED` | All ten test classes are `final`, same-package JUnit 5 and exercise real page subjects/domain values with no mocked subject or test-only constructor; the combined venture test was split into `QaVentureViewTest`, `QaVenturesPageTest` and `QaVenturePageTest`. |
| Exact-head CI acceptance | `PARTIAL` | Focused head `20dc0d4` has verified REQUIRED_ALL PASS (gate `qahouse-20261115-20dc0d4`, request `a0fa5028-9394-42fc-ad6d-0c34c94548ca`). The composed runtime staging root `51e79b0` (PR #11/#12 merge) is being gated once at its exact head; no composed-head PASS is claimed until that result is verified. |
| Staging composition | `CURRENT` | `.github/TAVALL_STAGING.md` now records PR #12 in the active composition with its exact merge/gate evidence. |
| Production behavior / design | `SOURCE_CANDIDATE` | The QA source has a typed venture page model and handler-backed `build(ITaskBoardVentureQueryHandler, ...)` factories for both venture pages; the shared Web controller still uses the legacy project-based adapter. Integration remains blocked by the repository-only scope of QA #38. |

The QA source now provides a typed venture record for the existing page model, including owner repositories and the recorded blocker. The model can render this record independently of project summaries, and `QaVenturesPage`/`QaVenturePage` expose handler-backed factories that resolve the directory and its engineering tasks through `ITaskBoardVentureQueryHandler`. The shared Web controller still invokes the legacy project-based adapter. The Web repository is read-only for issue #38; route integration and browser acceptance therefore remain unverified.

## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| 2026-11-15 | `LOCAL_VALIDATED` | Converted the remaining combined venture test into direct concrete-class tests and strengthened the two priority page tests with distinct real-behavior and failure-path methods. No production code changed. | Issue [tavall-web-qa#5](https://github.com/TavallStudios/tavall-web-qa/issues/5); `javac` compile of main + generated + test sources against pinned exact-source dependencies (Web `aa2f95a`, QA `1094b37`) followed by JUnit Platform launch: 30 tests found, 30 passed, 0 failed. | Exact-head `REQUIRED_ALL` gate and staging promotion remain pending. The Gradle `test` task could not finish locally because the shared composite `nodeSetup` task hung; the standalone exact-source compile/run above was used instead. |
| 2026-11-15 3:48 AM PST | `INTEGRATED` | Focused PR #12 merged into runtime staging root at `51e79b0`; resumed audit confirmed all ten page/route/product test classes are `final`, same-package JUnit 5 with real subjects and no mocked collaborators, the venture split and priority-page failure paths are intact, and `QaPageAnimations.build()` behavior is asserted through its owning pages. Recording PR #12 in the staging manifest and gating the composed exact head once. | Gate `qahouse-20261115-20dc0d4` (request `a0fa5028-9394-42fc-ad6d-0c34c94548ca`) REQUIRED_ALL PASS on focused head `20dc0d4`; `java_gate.py status --sha 20dc0d4` = PASS; `java_gate.py status --sha 51e79b0` = NONE (composed gate not yet run). | Composed exact-head gate enqueued once; promotion of staging PR #11 to main remains contingent on that verified PASS. No code gap found. |
| `2026-11-20 UTC` | `SOURCE_CANDIDATE` | Added a typed venture page-model factory with owner-repository tags, blocker display and task links back to `tavall-engineering`. Existing project-derived rendering retains its current tags and links for compatibility. | Issue #38; `:tavall-qa-web-frontend` burst module test, 32 tests passed, 0 failed; exact source based on QA staging head `48b9685`. | New route data is not wired by the shared Web controller; browser capture/deployed behavior is not verified and remains outside the QA-only scope. |
| `2026-11-25 UTC` | `SOURCE_CANDIDATE` | Documentation-compliance follow-up: removed the unused `QaApiRoutes` namespace so `QaRoutes` is the single compile-time route owner, added handler-backed `QaVenturesPage`/`QaVenturePage` factories over `ITaskBoardVentureQueryHandler`, and replaced venture literals with typed `TaskBoardProjectKey.ENGINEERING`/`TaskBoardRepositoryIdentity`. | Issue #39; `:tavall-qa-api` 27 tests, `:tavall-qa-system` 65 tests, `:tavall-qa-web-frontend` 35 tests, all passed with 0 failures against exact-source Web `aa2f95a`. | The shared Web controller still serves the project-derived page until its own consumer change; browser acceptance remains unverified. |

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PR_DRAFT` | `TavallStudios/tavall-qa/docs/progression/modules/TAVALL_QA_WEB_FRONTEND_PROGRESSION.md` | 2026-11-20 | [QA PR #39](https://github.com/TavallStudios/tavall-qa/pull/39), targeting staging root #34; route integration and browser acceptance pending. |
| Notion | `PENDING` | 1:1 twin of the GitHub module PROGRESSION | 2026-11-15 | External twin not updated in this tavall-qa-repository-only scope. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-15 | GitHub | `UPDATED` | Same path | Same path | issue tavall-web-qa#5 | Added canonical Current Status / Progression Timeline tables and the required collapsed Documentation Update State footer for the house-test conversion. |
| 2026-11-15 3:48 AM PST | GitHub | `UPDATED` | Same path | Same path | issue tavall-web-qa#5; staging root `51e79b0` | Recorded focused-head REQUIRED_ALL PASS, composed-head gate-pending state, and PR #12 in the active staging composition. |
| 2026-11-20 | GitHub | `UPDATED_PENDING_NOTION_SYNC` | `docs/progression/modules/TAVALL_QA_WEB_FRONTEND_PROGRESSION.md` | Former `tavall-web-qa` owner path | [QA PR #39](https://github.com/TavallStudios/tavall-qa/pull/39); 32 frontend page tests | Recorded typed venture page-model support, engineering task links and the Web-owned route/capture blocker. |
| 2026-11-25 | GitHub | `UPDATED_PENDING_NOTION_SYNC` | `docs/progression/modules/TAVALL_QA_WEB_FRONTEND_PROGRESSION.md` | Same path | Issue #39; 35 frontend page tests with exact-source Web `aa2f95a` | Recorded handler-backed venture page factories, removed dead `QaApiRoutes`, typed project/repository identity, and the unchanged Web-controller scope blocker. |

</details>
