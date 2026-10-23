# Tavall Web QA Progression

> **Document Type:** `PROGRESSION`
> **Scope:** `MODULE`
> **Module:** `tavall-web-qa` (`API`, secondary role `LIBRARY`)
> **Runtime owner:** `tavall-web-app`

## Current implementation

The presentation preserves the accepted Direction C/A/C design source and uses the existing Forge primitives. It now renders request-bound public summaries, assigned work, evidence, authorized staff forms, task acceptance, Human Verify, task editing, and threaded discussion. Form submissions include CSRF and explicit organization context; the controller and QA API recheck all authority.

WebQA PR #2 code is `4a668de0ee59ce792f7117f4c9bd5d7eb4dc803b`, with current head `a633b59538084d266b55fbf74671a512a28e1283` and QA API pinned to PR #1 head `516a77c21d06c6ac9c98511edcadd5a9fa1c213d`.

## Local validation

- Command: `./gradlew --no-daemon clean test` with local exact-source Web and QA composites.
- Result: 6 tests passed; 0 failures, 0 errors, 0 skips, including current accepted-evidence acceptance controls and stale-evidence suppression.
- `git diff --check` passes for the current candidate worktree.
- The Web application consumer test run passed 411 tests with 9 PostgreSQL-gated skips.

## Remaining gates

- Exact-source Tavall CI and merge review.
- Browser acceptance at 1440px desktop and 390px mobile against the completed shared-runtime composition.
- DEVELOPMENT and STAGING deployment, host routing, and public `qa.tavall.org` verification.
