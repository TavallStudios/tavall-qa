# Tavall QA Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
```

This branch is the persistent non-production repository integration target for Tavall QA task-board and execution-checkpoint work. Pull requests remain the work and review records; this file only identifies the repository integration root.

## Intended child PRs (not yet integrated)

| PR | Branch | Target base | Head | Scope |
|---|---|---|---|---|
| [#2](https://github.com/TavallStudios/tavall-qa/pull/2) | `docs/qa-board-examples-20261025` | `staging/platform` | `53370875e79594deba2505b451c22aecaf614720` | QA ownership and production examples documentation |
| [#3](https://github.com/TavallStudios/tavall-qa/pull/3) | `working/execution-checkpoints-01a175ae` | `staging/platform` | `e451f4f984b331a0add7c4398c961bea6e610ce0` | Task-owned leases, durable checkpoints, payloads, and source-guarded resume |

The listed heads are intended membership only. Neither child is integrated until merged into this branch. Run the required composed QA checks on the resulting staging head before considering promotion. Production database migration application, Web runtime acceptance, deployment, model invocation, and cross-provider continuation are separate gates.
