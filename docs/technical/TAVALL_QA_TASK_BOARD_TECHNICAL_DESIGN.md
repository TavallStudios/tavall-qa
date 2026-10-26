# Tavall QA and Task Board Technical Design

**Status:** Active completion design for Issue #263
**Canonical task and progress authority:** `TavallStudios/tavall-qa`
**Shared runtime owner:** Tavall Web application (`tavall-web-app`)
**Design authority:** Issue #263, the accepted Task Board decisions preserved in the current `tavall-web-qa` presentation, and the Tavall QA Staff Tiers I–V policy.

## Ownership map

| Capability | Owner | Boundary |
| --- | --- | --- |
| Task, section, project, QA state, assignments, evidence, history, and weighted progress | `tavall-qa` | One mapped-JPA PostgreSQL authority through `IPostgresDatabase.entities()`; checked-in schema migrations are owned here. |
| QA domain contracts and projections | `tavall-qa-api` | Typed domain values, requests, results, public/staff views, and handler interfaces. |
| QA implementation | `tavall-qa-system` | Mapped entities, query/mutation handlers, persistence adapter, and progress aggregation; no independent process or runtime. |
| Account identity and authorization facts | Tavall Account modules in `tavall-web-app` | SSO principal binding, current Account standing, explicit entitlement grants, organization membership/service binding, global authority, and separately linked Minecraft identity. |
| Host routing, HTTP authorization boundary, and Account-to-QA context resolution | `tavall-web-app` | Spring adapter in the existing Tavall Web shared runtime. Every request resolves current Account facts before querying or mutating QA state. |
| Accepted public/staff presentation | `tavall-web-qa` | Existing Forge public index, project and task pages, Focus Workspace C, and Message Editor. Presentation only; it owns neither authorization nor persistence. |
| Protected endpoint discovery | `tavall-web-dashboard` | Catalog entry for `qa.tavall.org`; it does not host the public QA product or grant access. |
| Historical game QA and design lineage | `tavall-mc` | Issue #263 and closed PRs #259/#274 preserve history and design evidence; they are not the current Task Board runtime or persistence owner. |

The dependency direction is:

```text
Tavall Web shared runtime
  ├── Tavall Account authority ──> current QaSubjectContext
  ├── tavall-qa-system ──> tavall-qa-api
  └── tavall-web-qa presentation ──> tavall-qa-api projections

tavall-web-dashboard ──> endpoint discovery only
PostgreSQL ──> sole durable Task Board and QA authority
```

The QA source module is classified as `SYSTEM` because it owns the Task Board capability and its implementation fan-in. Its module/artifact identity is `tavall-qa-system`; its runtime owner remains the existing Web application. A separate QA process, auth service, or database is not justified by the current lifecycle or consumers.

## Real production patterns and next QA Board integration

The current `tavall-qa` repository already has mapped-JPA and typed Task Board logic. Treat the following as authoritative **source examples**, not snippets to copy into a competing service:

- [`TaskBoardQueryHandler`](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/handler/TaskBoardQueryHandler.java) implements the `ITaskBoardQueryHandler` boundary, obtains `ITaskBoardDataHandler`, and emits public/staff projections. A new web/Notion adapter must request filtered views from this owner rather than query task rows directly.
- [`TaskBoardTaskEntity`](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/persistence/entity/TaskBoardTaskEntity.java) persists source reference, account/verification identity, task revisions and evidence linkage. Notion/GitHub synchronization should match stable external IDs to the owning QA task and handle revision conflicts, **not invent a second status authority**.
- [`TaskBoardMutationHandler`](https://github.com/TavallStudios/tavall-qa/blob/85779a99e8ab338c0366e3958d2a58da3023747b/tavall-qa-system/src/main/java/org/tavall/qa/handler/TaskBoardMutationHandler.java) is the owning mutation boundary. Do not create direct database writes in browser actions or provider hooks.

##### Why
Task acceptance, evidence and human verification must use one atomic, audited authority. A dashboard action, GitHub label or Notion checkbox does not independently establish QA acceptance.

### Approved October 2026 consumers

- **Task projection:** Notion planning tasks may map 1:1 to executable QA task identities, with GitHub issue/PR refs mirrored for traceability. Resolve conflicts and keep role/owner/source evidence; retain the older Linear operating model as migration history until reconciled.
- **WebMCP migration:** Proposed WebMCP task/QA views become Tavall Account-role-aware QA Board views. Existing `agent-webmcp` bounded service operations remain distinct until audited and explicitly migrated, not automatically deleted.
- **Agent provider continuity:** Open Harness may checkpoint provider sessions, while QA owns task state and evidence, Tavall AI owns roles, and Account controls scope.
- **Visual acceptance:** Web QA UI must reuse the approved product-specific Forge information architecture, with actual desktop/mobile screenshot and denied-state tests. No invented public progress, no implicit authorization from a hidden UI element.

### Acceptance gates

Verify actor/tenant grant on each read/write, source task revision and idempotent external synchronization, role-aware public/staff views, revocation and rejected evidence, migration `027`/`028` on a real ephemeral PostgreSQL test database, and same-source Web E2E. Record executed tests, exact SHA and runtime deployment in PROGRESSION rather than calling this design proof of availability.

## Authorization boundary

The authenticated Tavall Account principal is resolved to a stable Account ID. Each request reads the current Account status, active QA entitlement grants, linked Minecraft identity, and active organization context with the `tavall-web` service binding. QA runs inside the shared Web runtime, so it does not invent a separate `qa` service binding. QA class, tier, environment grants, task assignment, and capability remain independent facts. No Spring `ROLE_STAFF`, SSO provider, hostname, display name, or tier alone grants QA authority.

The Web adapter resolves those facts for every request and supplies a typed subject context. QA query and mutation handlers enforce that context again at their operation boundary. Missing or unavailable Account authority, revoked grants, inactive service bindings, disabled or merged Accounts, and unmatched task assignments fail closed. Account-global Superuser authority still requires an active Account, active QA service binding, linked Minecraft identity for Human Verify, valid evidence, and the task's domain invariants.

Tavall Account remains the authority for identity, Account standing, and explicit grants. Task Board assignments are task-scoped QA work records, not a second Account or role system. Promotions remain deliberate Account grants; VQC or task completion does not automatically grant a tier or capability.

## Persistence and migration ownership

`tavall-qa-system` owns mapped JPA entities and checked-in PostgreSQL migrations. The Web runtime registers the QA entity package in its existing Tavall Database composition and does not create an EntityManagerFactory, run runtime DDL, or duplicate QA schema files. QA handler wiring defers resolving the shared Database capability until the first handler call, after the Web composition root has registered its runtime-owned database. Migration `027_task_board.sql` owns the base project/task graph; `028_task_board_qa_workflows.sql` adds Account attribution, scoped assignments, evidence, append-only audit history, QA/QC eligibility, task revisions, and idempotent VQC attribution.

Evidence submission, evidence review, task acceptance, and Human Verify are separate mutations. Review can accept or reject evidence but awards no VQC. Task acceptance requires an accepted, current task fingerprint in the authorized environment, records `TASK_ACCEPTED`, sets the work status to `DONE`, and keeps QA state `NHV`. A qualifying accepted task adds one base VQC for the evidence submitter, uniquely keyed by task and Account. Human Verify remains a separate action requiring accepted current evidence, a current authorization check, active organization/service binding, and linked Minecraft identity. Rejection increments the task revision so older evidence cannot be reused. Material task changes invalidate prior verification and evidence fingerprints.

The legacy `progress_task` path and `publicProgressTaskId` synchronization remain retired. Public weighted totals filter both project and task audience before counts or aggregation, and exclude cancelled tasks. Public projections omit Account/Minecraft identities, private evidence, QA notes, privileged source references, and staff destinations.

## Durable execution leases and provider-neutral checkpoints

The implementation candidate in [QA #3](https://github.com/TavallStudios/tavall-qa/pull/3) keeps execution continuation under the existing task authority. [`TaskBoardExecutionCheckpointHandler`](../../tavall-qa-system/src/main/java/org/tavall/qa/handler/TaskBoardExecutionCheckpointHandler.java) implements its typed API with `@DelegatesTo`, generated `DependencyAccess` and the owning dependency generation. Each private read and mutation checks current Account/service context, DEV qualification/assignment or Account-global QA authority, and task/project visibility. A browser or provider must receive its subject context from the existing server-side Account resolver.

[`TaskBoardExecutionCheckpointDataHandler`](../../tavall-qa-system/src/main/java/org/tavall/qa/persistence/TaskBoardExecutionCheckpointDataHandler.java) consumes the checked-in Database `32aa272` entity API through `DependencyAccess<IPostgresDatabase>`. Its atomic operation receives only Tavall Database's typed entity context; Database retains transaction begin/flush/commit/rollback and EntityManager ownership. It uses the same [`TaskBoardTaskEntity.lockKey`](../../tavall-qa-system/src/main/java/org/tavall/qa/persistence/entity/TaskBoardTaskEntity.java) as ordinary task mutations, so a task revision cannot change between a checkpoint's fingerprint check and commit.

[`TaskBoardExecutionLeaseEntity`](../../tavall-qa-system/src/main/java/org/tavall/qa/persistence/entity/TaskBoardExecutionLeaseEntity.java) owns one current bounded work lease per task. A lease has an actor, stable run/token identity, revision, checkpoint sequence, task fingerprint and expiry. Duplicate acquisition does not extend a lease. An expired token cannot be revived; a newly authorized token can resume the same run at its latest checkpoint sequence. Renewal and append use revision checks, and changed task requirements reject continuation. These durable identities belong in PostgreSQL; Registry or Cache cannot replace their authority.

[`TaskBoardExecutionCheckpointEntity`](../../tavall-qa-system/src/main/java/org/tavall/qa/persistence/entity/TaskBoardExecutionCheckpointEntity.java) maps append-only history. A checkpoint records exact repository/branch/commit/diff identity, provider/state observation and a content-addressed Storage payload digest. Equal retries return the original checkpoint without another audit event; conflicting payloads or stale writers reject. Mapped timestamps use PostgreSQL microsecond precision before being returned, preserving revision equality after a database round trip. [Migration 034](../../tavall-qa-system/src/main/resources/schema/postgres/034_task_board_execution_checkpoints.sql) owns the new tables, sequence constraint, append-only triggers and audit events. The shared Web runtime must separately register and apply the migration through its existing owner.

The immutable payload remains responsible for task instructions, acceptance conditions, exact policy/doc receipts, inspected/modified source, operations, CI/artifact references, capabilities, errors and the next executable action. A digest in a checkpoint is only a reference: the execution host must publish and read back the actual Storage bytes, verify the digest and current source state, re-evaluate grants, then select an authorized provider before resuming. The current candidate does not yet implement that complete lifecycle or its Web transport. `COMPLETED` execution is an observation and does not accept a task, award VQC or grant Human Verify. Checkpoints are excluded from public Task Board projections.

[`TaskBoardExecutionPostgresIntegrationTest`](../../tavall-qa-test-suite/src/test/java/org/tavall/qa/handler/TaskBoardExecutionPostgresIntegrationTest.java) is the required acceptance path for native private PostgreSQL, actual Database mappings and scoped DI. It covers idempotency, concurrent claims/writers, source-task invalidation, expiry, current authorization, append-only SQL enforcement and restart recovery. Its explicit integration task requires the fixture instead of accepting a skip. Execution results belong in module Progression; test source alone is not acceptance.

## Existing product surface

The current `tavall-web-qa` source already carries the accepted Direction C main board, Direction A project/task detail, and Direction C Focus Workspace. This work keeps those layouts and their current Forge primitives. Changes are limited to safe projections, working authorization-aware forms, evidence/history display, and responsive behavior already specified by the accepted design.

Historical Task Board UI and PR lineage under `tavall-mc` are reference material only. The current product route is `https://qa.tavall.org` on the Tavall Web shared runtime; `/task-board/**` compatibility remains an explicit redirect.

## Integration boundaries and acceptance

The current progress aggregator computes weighted public progress inside the QA authority. Cache/event publication, CMS widgets, roadmap projections, curated Discord milestones, and public receipts must attach to existing Tavall owners through typed contracts after those owners and consumers are verified. No placeholder subscriber or duplicate progress store is introduced. The PvP QA repository and PRs referenced as #155 and #179–182 must be verified by repository identity before any source or VQC progression is treated as authoritative.

Acceptance binds to exact source SHAs for each participating repository and the actual Tavall Web DEVELOPMENT/STAGING runtime. Required checks include direct HTTP privacy and authorization, mapped-JPA migration/persistence behavior, task assignment/evidence/review/acceptance/Human Verify and invalidation, desktop/mobile browser behavior at the accepted layouts, shared runtime composition, and health/routing. Source, CI, artifact, migration, deployment, DNS, and public browser acceptance are recorded separately; none is inferred from the presence of a host config or a local build. Current local source-test results are recorded in the module progression documents; PostgreSQL, remote CI, browser, and deployment gates are tracked independently.

<details>
<summary>Documentation Update State</summary>

### Current Locations
| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `1:1_DRAFT_PR` | `TavallStudios/tavall-qa/docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md` | 2026-10-25 2:17 AM PDT | [Draft PR](https://github.com/TavallStudios/tavall-qa/pull/2); linked Notion twin |
| Notion | `1:1_DRAFT_PR` | [Notion twin](https://app.notion.com/p/40438458ddfd8142b3fef192175a7bfa) | 2026-10-25 2:17 AM PDT | Created Notion twin https://app.notion.com/p/40438458ddfd8142b3fef192175a7bfa |

### Update History
| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-10-25 2:17 AM PDT | GitHub | `UPDATED` | `docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md` | Same file | Branch `docs/qa-board-examples-20261025` | Added verified source references, current design scope and footer. |

| 2026-10-26 11:10 AM PDT | GitHub | `UPDATED` | `docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md` | Same logical document from PR #2 | PR #3, source `fd44ada` and exact CI receipts | Documented real DI/Database checkpoint implementation; kept runtime, payload and provider-continuation gates separate. |
</details>
