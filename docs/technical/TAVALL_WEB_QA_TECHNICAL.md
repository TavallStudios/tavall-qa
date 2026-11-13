# Tavall Web QA Surface Technical Record

**Document Type:** `TECHNICAL DESIGN`
**Scope:** Browser presentation module
**Runtime owner:** `tavall-web-app`
**Domain authority:** `TavallStudios/tavall-qa`

## Ownership

This repository owns Forge presentation and route constants for the QA product. It owns no authentication, authorization, persistence, runtime, or independent server. The shared Web application resolves current Tavall Account context. `tavall-qa` owns the mapped-JPA project/task/evidence/assignment/audit/progression authority.

The owner map, dependency boundaries, migration ownership, and security model are recorded in the canonical [Task Board technical design](https://github.com/TavallStudios/tavall-qa/blob/main/docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md).

## Preserved design source

This implementation retains the accepted design already present in the product source (`main@b50ab5a`) and the Task Board decisions in [Issue #263](https://github.com/TavallStudios/tavall-mc/issues/263): Direction C public main board, Direction A project/task detail, Direction C staff Focus Workspace, and the existing Message Editor. This record describes implementation ownership and behavior; it does not introduce a replacement visual design.

## Existing Message Editor owner

The message editor is owned by the shared Tavall Web application through its existing `MessageAdminController` and `IMessageAdminService`. The QA `/messages/{key}` route is only a capability-gated handoff; it does not render or persist a parallel editor. The shared Web route rechecks either the current Account `mc.messages.edit` module permission or the explicit QA `MANAGE_QA_MESSAGES` capability under the active Tavall Web service binding. The QA capability is resolved again on the editor GET and save POST; message reads/saves continue through the canonical Message Admin service. The desktop A index/form/preview composition and mobile C Preview/Edit/Messages focus modes are implemented in that shared Web editor.

## Request-specific form data

The Web controller builds typed page contexts from the current resolved `QaSubjectContext`, organization, CSRF token, and exact task evidence projection. Pages use those facts to render applicable forms. Every submitted form is still re-authorized by the Web adapter and QA mutation handler. Form visibility is presentation only.

The staff workspace offers Overview, QA, Edit, and Thread focus modes while retaining URL-hash restoration. QA forms support evidence submission, review, task acceptance, rejection, separate Human Verify, scoped assignment, and revocation. The task detail page exposes only the assigned/public-safe fields appropriate to the resolved subject. Public visitors never receive private Account identities, internal QA notes, or privileged action destinations.

Page behavior stays with this presentation module: `src/main/ts/qa-board.ts` owns the existing progressive task expansion, staff select enhancement, and Focus Workspace interactions. `QaBoardDomDataRef` supplies the namespaced project task-group/item/expander hooks, and `QaPageAnimations` composes the typed initializer with the shared Forge reveal animation through the frontend `PageAnimationPlan`; it adds no script contract to the Web API and does not alter the approved layout or styling.

## Build and module boundary

The repository remains one API/LIBRARY presentation module because it provides reusable pages to the existing Tavall Web runtime and has no runtime lifecycle. Tests remain with this module; a second test module is not justified. The module consumes exact-source Tavall Web frontend/API contracts and the `tavall-qa-api` request/projection contracts.


## Generic live design review — November 13, 2026

The QA presentation reads the review surface and candidate option maps from the typed Task Board projection. It renders live sandboxed frames, exposes only candidate-declared option values, and provides a cross-task list of open reviews visible to the current staff subject. Winner forms remain server-authorized and CSRF-protected. The implementation has no dashboard product dependency; dashboard A/B/C is the first registered use. Local page tests, exact hosted checks, browser screenshots and runtime acceptance remain pending for the current source.
