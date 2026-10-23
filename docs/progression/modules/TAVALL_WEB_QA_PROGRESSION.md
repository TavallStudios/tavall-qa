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

## Remaining gates

- Exact-source Tavall CI and merge review.
- Browser acceptance at 1440px desktop and 390px mobile against the completed shared-runtime composition.
- DEVELOPMENT and STAGING deployment, host routing, and public `qa.tavall.org` verification.
