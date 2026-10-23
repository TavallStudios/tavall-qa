# tavall-qa-system

Canonical in-process Task Board and QA authority implementation.

## Responsibility

### Owns

- Mapped-JPA Task Board entities and checked-in PostgreSQL migrations.
- Task/project queries, mutations, scoped assignments, evidence review, immutable history, and weighted progress.
- The `tavall-qa-api` SYSTEM composition contract.

### Does Not Own

- A Spring service/process or Tavall Database connection lifecycle.
- Tavall Account identities, standing, or durable entitlement grants.
- A duplicate `progress_task` database or `publicProgressTaskId` synchronization.
- Cache, CMS, roadmap, and Discord subscribers before their owning contracts are verified.

## Repository Structure

```text
tavall-qa/
├── tavall-qa-api/
├── **tavall-qa-system/ ← This Module**
└── tavall-qa-test-suite/
```

## Relationships

| Module / System | Relationship |
| --- | --- |
| `tavall-qa-api` | Typed public contracts and projections implemented by this SYSTEM. |
| `tavall-qa-test-suite` | Exercises this module through production handlers and the API contract. |
| `tavall-web-app` | Composition/runtime owner; registers this module's entity package with the shared Tavall Database runtime. |
| Tavall Account | Supplies current Account facts; QA does not duplicate identity or authorization state. |
| `tavall-web-qa` | Renders public-safe and scoped assigned-task views; no persistence or authorization authority. |

This SYSTEM has no independently controllable FEATURE child modules. Its handlers and mapped entities share one domain and lifecycle, so they remain together.

## Documentation

| Type | Document | Purpose |
| --- | --- | --- |
| Technical | [`../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md`](../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md) | Authority, module, database, and runtime boundaries. |
| `PROGRESSION` | [`../docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md) | SYSTEM capability, persistence, validation, and consumer integration state. |

## Deployment

> This SYSTEM is not independently deployed.

Runtime owner: `tavall-web-app`. Current runtime integration: Tavall Web PR #76. The shared runtime composes this module and owns host routing and lifecycle.

## Development

- **Module Type:** `SYSTEM` (secondary role: `PROVIDER` for the installed task data capability)
- **Runtime:** `tavall-web-app`
- **Composition owner:** `tavall-web-app` through Tavall QA configuration
- **Contract owner and import direction:** `tavall-qa-system` implements `tavall-qa-api`; Web and Forge consumers depend on the API.
- **CI Definition:** [`.tavallci/ci.yaml`](.tavallci/ci.yaml)
- **Current PR Stack:** `working/qa-board-authority-hardening-20261023`; consumer integration in Tavall Web PR #76.
- **Runtime PR:** Tavall Web PR #76.
- **Progression:** [`../docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md)
- **Schema ownership:** `schema/postgres/027_task_board.sql` and `028_task_board_qa_workflows.sql`.
