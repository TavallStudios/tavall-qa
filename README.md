# Tavall QA

Authoritative backend task, progress, and QA verification plane for Tavall.

## Authority

`tavall-qa` is the canonical **mapped-JPA Task Board and QA verification authority**. It owns:
- Project lifecycle and audience definitions.
- Task graph, section mapping, priority, weight, and status.
- QA state (`NHV`, `REVIEW_REQUIRED`, `HUMAN_VERIFIED`), verifier identity, and timestamps.
- Action targets, discussion/suggestion/QA-note threads.
- Task-scoped QA assignments, evidence review, audit history, and VQC contributions.
- PostgreSQL schema migrations (`schema/postgres/027_task_board.sql` and `028_task_board_qa_workflows.sql`).
- Public and staff query projections.

## Architecture

- **`tavall-qa-api`**: Typed domain records, enums, request contracts, view projections, and handler interfaces. Primary type `API`; secondary role `LIBRARY`; runtime owner `tavall-web-app`.
- **`tavall-qa-system`**: Mapped JPA entities, `IPostgresDatabase` integration, mutation/query handlers, action target resolver, and SQL migrations. Primary type `SYSTEM`; secondary role `PROVIDER`; runtime owner `tavall-web-app`.
- **`tavall-qa-test-suite`**: Unit and integration coverage for queries, mutations, assignments, evidence, verification, invalidation, and authorization. Primary type `TEST_SUITE`; runtime owner `None`.

## Principles

1. **NHV Default**: All tasks enter as `NHV` (Not Human Verified).
2. **Acceptance and Human Verify are separate**: Acceptance requires current accepted evidence and marks the task `DONE`; it remains `NHV` until a separate authorized Human Verify with linked Minecraft identity.
3. **One VQC per accepted QA/QC task**: Evidence review alone earns no VQC. The accepted task operation awards at most one base VQC per task and Account, without automatic promotion or capability grants.
4. **Invalidation**: Material changes to task requirements, source, audience, weighting, QA/QC eligibility, or reopening a completed task invalidate the previous task fingerprint and Human Verify.
5. **Single Source of Truth**: PostgreSQL mapped JPA authority; no duplicate writable progress database or legacy `ProgressTask` synchronization.

## Development

- **Current PR stack**: [QA authority PR #1](https://github.com/TavallStudios/tavall-qa/pull/1), then WebQA presentation PR #2 and Tavall Web consumer PR #76.
- **Runtime PR**: Tavall Web PR #76 composes this API/SYSTEM into the existing Web application runtime.
- **CI**: Root and module `.tavallci/ci.yaml` files identify the exact Gradle tasks owned by each source/build module.

## Documentation

- [Task Board technical ownership and security](docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md)
- [API progression](docs/progression/modules/TAVALL_QA_API_PROGRESSION.md)
- [SYSTEM progression](docs/progression/modules/TAVALL_QA_SYSTEM_PROGRESSION.md)
- [Test Suite progression](docs/progression/modules/TAVALL_QA_TEST_SUITE_PROGRESSION.md)

## Documentation

- **Technical Design**: [`docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md`](docs/technical/TAVALL_QA_TASK_BOARD_TECHNICAL_DESIGN.md)
- **Module Progression**: [`docs/progression/modules/`](docs/progression/modules/)
