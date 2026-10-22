# Tavall Web QA

The Tavall QA Board / QA Tracker browser product at `qa.tavall.org`. This is a **Tavall Web product surface**, not a second Spring service, task database, identity provider, or verification authority.

## Ownership

- `tavall-web-qa` — QA Forge browser presentation, stable route constants, and read-only projection-facing page composition (**primary module type: API; secondary role: LIBRARY**; runtime owner: `tavall-web-app`).
- `tavall-web-qa-test-suite` — separate canonical architecture validation (**primary module type: TEST_SUITE; runtime: None**).
- `tavall-web-app` — shared Spring host, `qa.tavall.org` virtual host routing, session/authentication, Tavall Account / organization authorization, and trusted task projection adapters.
- Existing mapped-JPA Tavall Web Task Board — canonical task/evidence/progress mutation authority. No duplicate writable `ProgressTask` or QA task table.

The public `/ → /projects/{project} → /tasks/{task}` journey and staff Workspace C remain the intended UI direction. `Done` does not imply `Human Verified`; no anonymous user can mutate task or evidence state. The first repository slice is a preview scaffold, **not** a live QA board.

## Build modules

- [QA product module](tavall-web-qa/README.md)
- [QA test suite](tavall-web-qa-test-suite/README.md)
- [QA module DESIGN](docs/design/TAVALL_WEB_QA_MODULE_DESIGN.md)
- [QA system PROGRESSION](docs/progression/TAVALL_WEB_QA_SYSTEM_PROGRESSION.md)

Java 25 / Gradle 9.6.1, exact-source Tavall CI, `org.tavall.web.frontend` and `org.tavall.architecture-tests` supplied by their canonical producers.

## Canonical architecture

Read [module separation](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/code-architecture/MODULE_SEPARATION.md), [module types](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/MODULE_TYPES.md), and [Tavall Web](https://github.com/TavallStudios/tavall-web). UI state must reflect the verified backend/Tavall Account projection; hiding staff controls is never authorization.

> **Status:** Repository setup in progress. This README initializes a previously empty repository; feature integration, hosted CI, signed-in actions, task persistence, `qa.tavall.org` DNS/ingress, and deployment are not claimed.
