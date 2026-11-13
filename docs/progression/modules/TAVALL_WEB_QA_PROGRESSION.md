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
