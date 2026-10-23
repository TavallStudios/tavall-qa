# tavall-qa-api

Caller-facing typed contracts for the Tavall QA and Task Board authority.

## Responsibility

### Owns

- QA/task domain values, requests, results, public/staff projections, and handler interfaces.
- The stable API consumed by the QA SYSTEM and Web presentation.

### Does Not Own

- JPA entities, PostgreSQL migrations, or transaction lifecycle.
- Tavall Account identity, QA grant storage, or Web host routing.
- A runtime, cache, CMS, roadmap, or Discord publisher.

## Repository Structure

```text
tavall-qa/
├── **tavall-qa-api/ ← This Module**
├── tavall-qa-system/
└── tavall-qa-test-suite/
```

## Relationships

| Module / System | Relationship |
| --- | --- |
| `tavall-qa-system` | Implements the QA API contracts and owns the Task Board authority. |
| `tavall-web-app` | Shared runtime owner and authenticated HTTP consumer. |
| `tavall-web-qa` | Forge presentation consumer of safe page projections. |

## Documentation

| Type | Document | Purpose |
| --- | --- | --- |
| Technical | [`../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md`](../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md) | Source, module, identity, and runtime ownership. |
| `PROGRESSION` | [`../docs/progression/modules/TAVALL_QA_API_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_API_PROGRESSION.md) | Contract implementation and consumer acceptance. |

## Deployment

> This API is not independently deployed.

Runtime owner: `tavall-web-app`. Current runtime integration: Tavall Web PR #76.

## Development

- **Module Type:** `API`
- **Secondary Roles:** `LIBRARY`
- **Runtime:** `tavall-web-app`
- **Composition owner:** `tavall-qa-system`
- **Contract owner and import direction:** `tavall-qa-system`, `tavall-web-app`, and `tavall-web-qa` depend on these caller-facing contracts.
- **CI Definition:** [`.tavallci/ci.yaml`](.tavallci/ci.yaml)
- **Current PR Stack:** QA authority hardening branch; Web consumer PR #76.
- **Progression:** [`../docs/progression/modules/TAVALL_QA_API_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_API_PROGRESSION.md)
