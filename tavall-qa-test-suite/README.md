# tavall-qa-test-suite

Test-only validation for the Task Board API and QA SYSTEM.

## Responsibility

### Owns

- Regression coverage for audience filtering, Account-scoped authorization facts, task assignments, evidence review, task acceptance, Human Verify, invalidation, audit behavior, progress, and checked-in migration contracts.
- Isolated PostgreSQL acceptance for checked-in migrations, Tavall Database mapped persistence, transaction rollback, concurrent acceptance retry/idempotency, stale writes, and restart recovery.

### Does Not Own

- Production behavior, persistence, migrations, or runtime lifecycle.
- A separately deployed test service.

## Repository Structure

```text
tavall-qa/
├── tavall-qa-api/
├── tavall-qa-system/
└── **tavall-qa-test-suite/ ← This Module**
```

## Relationships

| Module / System | Relationship |
| --- | --- |
| `tavall-qa-api` | Contracts exercised by this suite. |
| `tavall-qa-system` | Production implementation under test. |
| `tavall-web-app` | Consumer/runtime acceptance is separately owned by the Web application suite. |

## Documentation

| Type | Document | Purpose |
| --- | --- | --- |
| Technical | [`../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md`](../docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md) | System and persistence contract. |
| `PROGRESSION` | [`../docs/progression/modules/TAVALL_QA_TEST_SUITE_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_TEST_SUITE_PROGRESSION.md) | Coverage boundary and exact execution evidence. |

## Deployment

> This module is not independently deployed.

Runtime owner: `None`; the suite executes validation tasks only.

## Development

- **Module Type:** `TEST_SUITE`
- **Runtime:** `None`
- **Composition owner:** Gradle test execution against `tavall-qa-api` and `tavall-qa-system`.
- **CI Definition:** [`.tavallci/ci.yaml`](.tavallci/ci.yaml)
- **PostgreSQL integration:** Set `TAVALL_QA_TEST_POSTGRES_URL`, `TAVALL_QA_TEST_POSTGRES_USER`, and optionally `TAVALL_QA_TEST_POSTGRES_PASSWORD` to a disposable database before running `test`. When no URL is configured the isolated-database test is reported skipped; never point it at the shared Tavall Cloud database.
- **Current PR Stack:** QA authority hardening branch; consumer validation is carried by Tavall Web PR #76.
- **Progression:** [`../docs/progression/modules/TAVALL_QA_TEST_SUITE_PROGRESSION.md`](../docs/progression/modules/TAVALL_QA_TEST_SUITE_PROGRESSION.md)
