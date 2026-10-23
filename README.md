# Tavall Web QA

The Tavall QA Board / QA Tracker browser product at `qa.tavall.org`. This is a **Tavall Web product surface**, not a second Spring service, task database, identity provider, or verification authority.

## Ownership

- `tavall-web-qa` — QA Forge browser presentation and stable route contracts (**primary module type: API; secondary role: LIBRARY**; runtime owner: `tavall-web-app`). The repository currently has one production module; its tests remain with that module.
- `tavall-web-app` — shared Spring host, `qa.tavall.org` virtual host routing, session/authentication, Tavall Account / organization authorization, and trusted task projection adapters.
- Authoritative backend task authority — `TavallStudios/tavall-qa` (`tavall-qa-api`, `tavall-qa-system`), canonical mapped-JPA task/evidence/progress authority. No duplicate writable `ProgressTask` or QA task table.

The current implementation preserves the recovered public Direction C main board, Direction A project/task pages, and Direction C Focus Workspace. The accepted source is the existing presentation at `main@b50ab5a`; this completion adds request-bound workflows to those pages without replacing their visual direction. `DONE` does not imply `HUMAN_VERIFIED`; acceptance and Human Verify are separate actions. Public pages use sanitized projections, and the Web/QA boundaries check current Account facts and task assignments on each mutation. Runtime deployment and public acceptance remain separate gates.

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
- **Current PR Stack:** WebQA presentation PR #2 code `4a668de0ee59ce792f7117f4c9bd5d7eb4dc803b`, current head `a633b59538084d266b55fbf74671a512a28e1283`, pinned to QA authority PR #1 head `516a77c21d06c6ac9c98511edcadd5a9fa1c213d`; consumed by Tavall Web PR #76.
- **Progression:** [`docs/progression/modules/TAVALL_WEB_QA_PROGRESSION.md`](docs/progression/modules/TAVALL_WEB_QA_PROGRESSION.md).
