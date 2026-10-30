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

## Authorization boundary

The authenticated Tavall Account principal is resolved to a stable Account ID. Each request reads the current Account status, active QA entitlement grants, linked Minecraft identity, and active organization context with the `tavall-web` service binding. QA runs inside the shared Web runtime, so it does not invent a separate `qa` service binding. QA class, tier, environment grants, task assignment, and capability remain independent facts. No Spring `ROLE_STAFF`, SSO provider, hostname, display name, or tier alone grants QA authority.

The Web adapter resolves those facts for every request and supplies a typed subject context. QA query and mutation handlers enforce that context again at their operation boundary. Missing or unavailable Account authority, revoked grants, inactive service bindings, disabled or merged Accounts, and unmatched task assignments fail closed. Account-global Superuser authority still requires an active Account, active QA service binding, linked Minecraft identity for Human Verify, valid evidence, and the task's domain invariants.

Tavall Account remains the authority for identity, Account standing, and explicit grants. Task Board assignments are task-scoped QA work records, not a second Account or role system. Promotions remain deliberate Account grants; VQC or task completion does not automatically grant a tier or capability.

## Persistence and migration ownership

`tavall-qa-system` owns mapped JPA entities and checked-in PostgreSQL migrations. The Web runtime registers the QA entity package in its existing Tavall Database composition and does not create an EntityManagerFactory, run runtime DDL, or duplicate QA schema files. QA handler wiring defers resolving the shared Database capability until the first handler call, after the Web composition root has registered its runtime-owned database. Migration `027_task_board.sql` owns the base project/task graph; `028_task_board_qa_workflows.sql` adds Account attribution, scoped assignments, evidence, append-only audit history, QA/QC eligibility, task revisions, and idempotent VQC attribution.

Evidence submission, evidence review, task acceptance, and Human Verify are separate mutations. Review can accept or reject evidence but awards no VQC. Task acceptance requires an accepted, current task fingerprint in the authorized environment, records `TASK_ACCEPTED`, sets the work status to `DONE`, and keeps QA state `NHV`. A qualifying accepted task adds one base VQC for the evidence submitter, uniquely keyed by task and Account. Human Verify remains a separate action requiring accepted current evidence, a current authorization check, active organization/service binding, and linked Minecraft identity. Rejection increments the task revision so older evidence cannot be reused. Material task changes invalidate prior verification and evidence fingerprints.

The legacy `progress_task` path and `publicProgressTaskId` synchronization remain retired. Public weighted totals filter both project and task audience before counts or aggregation, and exclude cancelled tasks. Public projections omit Account/Minecraft identities, private evidence, QA notes, privileged source references, and staff destinations.

## Existing product surface

The current `tavall-web-qa` source already carries the accepted Direction C main board, Direction A project/task detail, and Direction C Focus Workspace. This work keeps those layouts and their current Forge primitives. Changes are limited to safe projections, working authorization-aware forms, evidence/history display, and responsive behavior already specified by the accepted design.

Historical Task Board UI and PR lineage under `tavall-mc` are reference material only. The current product route is `https://qa.tavall.org` on the Tavall Web shared runtime; `/task-board/**` compatibility remains an explicit redirect.

## Integration boundaries and acceptance

The current progress aggregator computes weighted public progress inside the QA authority. Cache/event publication, CMS widgets, roadmap projections, curated Discord milestones, and public receipts must attach to existing Tavall owners through typed contracts after those owners and consumers are verified. No placeholder subscriber or duplicate progress store is introduced. The PvP QA repository and PRs referenced as #155 and #179–182 must be verified by repository identity before any source or VQC progression is treated as authoritative.

Acceptance binds to exact source SHAs for each participating repository and the actual Tavall Web DEVELOPMENT/STAGING runtime. Required checks include direct HTTP privacy and authorization, mapped-JPA migration/persistence behavior, task assignment/evidence/review/acceptance/Human Verify and invalidation, desktop/mobile browser behavior at the accepted layouts, shared runtime composition, and health/routing. Source, CI, artifact, migration, deployment, DNS, and public browser acceptance are recorded separately; none is inferred from the presence of a host config or a local build. Current local source-test results are recorded in the module progression documents; PostgreSQL, remote CI, browser, and deployment gates are tracked independently.

## Notion 1:1 Task Mapping and WebMCP Task Views

**Authority:** Accepted Phase 6 platform integration decision (2026-10-30). Notion twin: [https://app.notion.com/p/40438458ddfd8142b3fef192175a7bfa](https://app.notion.com/p/40438458ddfd8142b3fef192175a7bfa).

QA holds no Notion credentials and runs no recurring polling loop. External synchronization is driven by authorized agents (the orchestrator using its Notion connector) calling QA's owner APIs:

1. **Idempotent Ingestion (`ITaskBoardNotionSyncHandler.ingestSnapshot`):**
   - Agent submits typed `NotionTaskSnapshot` (workspace, database, page id, page URL, `last_edited_time` revision, mapped properties, archived/deleted flag).
   - Resolves or creates exactly one QA task identity, keyed idempotently by Notion page ID.
   - Per-database configurable mapping (`NotionDatabaseTaskMapping`): canonical schemas for Tavall PvP Tasks (`3a338458ddfd80eb9bbfcb8bc741e610`) and Kingdoms Tasks (`b3f34d39223b43378a60531cd7e6fdbf`), with support for custom database property mappings.
   - Status mapping: Not started → BACKLOG, Up next → READY, In progress → ACTIVE, Done → DONE, Blocked → BLOCKED.
   - Replay safety: snapshots with same or older `last_edited_time` are idempotent no-ops.
   - Conflict resolution: when QA has pending unacknowledged outbound changes and Notion concurrently edits, conflict details are recorded in `NotionTaskSyncState` and deterministic resolution applies (QA DONE/HUMAN_VERIFIED execution state takes precedence; planning fields are updated).
   - Archive/deleted handling: sets task status to CANCELLED and marks sync state archived/deleted.

2. **Outbound Pending Export and Acknowledgement:**
   - Pending outbound changes are exported via `findPendingOutboundChanges(subject, databaseId)` with monotonically increasing `syncRevision`.
   - Agent acknowledges successful write to Notion via `acknowledgeOutboundSync(subject, request)`.
   - Replay safety: stale or duplicate acknowledgements are safe no-ops.

3. **File-Backed Durable State (`NotionTaskSyncState`):**
   - Managed JPA entity `NotionTaskSyncStateEntity` and PostgreSQL migration `035_task_board_notion_sync.sql`.
   - File redundancy mirror via `tavall-filesystem` (`FilesystemAtomicOperations`) at `<mirrorRoot>/qa/notion-sync/<pageId>.json` guarantees state survives restarts independently of PostgreSQL.

4. **Agent-Facing Role-Aware Task Views (`ITaskBoardAgentViewHandler`):**
   - Role-aware task discovery (`findAuthorizedAgentTasks`): filters tasks by explicit Account capability and task assignment.
   - Bounded task context (`findAgentTaskContext`): returns task-specific execution, evidence, comments, actions, and Notion planning context without dumping the workspace or project.
   - Execution updates (`updateAgentExecution`): authorized agents update execution status, add notes, and advance sync revision through owner APIs.
