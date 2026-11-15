# Tavall QA Web Frontend

The Tavall QA Board / QA Tracker browser product at `qa.tavall.org`. This is a **Tavall Web presentation module hosted in Tavall QA**, not a second Spring service, task database, identity provider, or verification authority.

## Ownership

- `tavall-qa-web-frontend` — QA Forge browser presentation and stable route contracts (**primary module type: FEATURE; secondary role: PROVIDER**; runtime owner: `tavall-web-app`). Published under artifact coordinate `tavall-web-qa` for consumer compatibility with `tavall-web-app`.
- `tavall-web-app` — shared Spring host, `qa.tavall.org` virtual host routing, session/authentication, Tavall Account / organization authorization, and trusted task projection adapters.
- Authoritative backend task authority — `TavallStudios/tavall-qa` (`tavall-qa-api`, `tavall-qa-system`), canonical mapped-JPA task/evidence/progress authority. No duplicate writable `ProgressTask` or QA task table.

The current implementation preserves the recovered public Direction C main board, Direction A project/task pages, and Direction C Focus Workspace. The accepted source is the existing presentation at `main@b50ab5a`; request-bound workflows and typed page interactions are already part of that implementation. `DONE` does not imply `HUMAN_VERIFIED`; acceptance and Human Verify are separate actions. Public pages use sanitized projections, and the Web/QA boundaries check current Account facts and task assignments on each mutation. This follow-up removes the unused static editor stub and keeps the route handoff to the existing shared Web Message Editor; it adds no replacement design or message store. Runtime deployment and public acceptance remain separate gates.

The message editor remains owned by the shared Tavall Web application. This module keeps the `/messages/{key}` route contract so the Account and QA authorization adapter can forward an authorized edit to the existing editor and its canonical message service; it does not ship a second message editor or message store.

The route journey is `/` → `/projects/{projectKey}` → `/projects/{projectKey}/tasks/{taskId}`. Staff Focus Workspace opens at `/workspace/{projectKey}/tasks/{taskId}` for a selected authorized task.

## Build modules

- [QA product module](README.md)
- [QA module PROGRESSION](docs/progression/modules/TAVALL_WEB_QA_PROGRESSION.md)
- [QA surface TECHNICAL](docs/technical/TAVALL_WEB_QA_TECHNICAL.md)
- [Canonical task authority TECHNICAL](https://github.com/TavallStudios/tavall-qa/blob/main/docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md)

Java 25 / Gradle 9.6.1, exact-source Tavall CI, `org.tavall.web.frontend` and `org.tavall.architecture-tests` supplied by their canonical producers.

## Canonical architecture

Read [module separation](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/code-architecture/MODULE_SEPARATION.md), [module types](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/MODULE_TYPES.md), and [Tavall Web](https://github.com/TavallStudios/tavall-web). UI state must reflect the verified backend/Tavall Account projection; hiding staff controls is never authorization.

## Development

- **Module Type:** `API` (secondary role: `LIBRARY`).
- **Runtime:** `tavall-web-app`; this repository owns no server or database lifecycle.
- **Composition owner:** The shared Tavall Web application consumes this presentation module and the canonical `tavall-qa-api` projections.
- **CI Definition:** [`.tavallci/ci.yaml`](.tavallci/ci.yaml).
- **Current PR Stack:** WebQA PR #2 is merged at `5943bbd7ba8b576fb50c64de3b1b6537d30f77a4`; this follow-up removes its unused static message-editor stub. The shared Web app routes authorized QA message edits to its canonical editor and message service.
- **Progression:** [`docs/progression/modules/TAVALL_WEB_QA_PROGRESSION.md`](docs/progression/modules/TAVALL_WEB_QA_PROGRESSION.md).
