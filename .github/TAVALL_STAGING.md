# Tavall QA Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform-v2
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform-v2
```

This branch is the persistent repository-level integration root for Tavall QA (Generation 2). Normal QA pull requests target `staging/platform-v2`. Pull requests remain the work and review records; this manifest tracks active composition and historical reconciliation.

## Historical Reconciliation (`NEEDS_RECONCILIATION`)

The following pull requests were merged directly into `main` without passing through staging ancestry between 2026-11-01 and 2026-11-07:

| PR | Branch | Head SHA | Merged At (UTC) | Description |
|---|---|---|---|---|
| [#17](https://github.com/TavallStudios/tavall-qa/pull/17) | `working/wui-qa-visible-board-20261106` | `0cfc342` | 2026-11-06T23:07:00Z | feat(qa): subject-visible board and project views |
| [#16](https://github.com/TavallStudios/tavall-qa/pull/16) | `working/wdocs-progression-20261106` | `fd0c3e2` | 2026-11-06T07:16:00Z | docs(progression): backfill merged 2026-11-04..06 PRs for tavall-qa |
| [#14](https://github.com/TavallStudios/tavall-qa/pull/14) | `working/engineering-project-seed-20261105` | `f55d32e` | 2026-11-05T17:10:00Z | Add: seed admin-only tavall-engineering Task Board project |
| [#13](https://github.com/TavallStudios/tavall-qa/pull/13) | `working/wtqa-test-suite-house-tests-20261105` | `c6cdfc8` | 2026-11-05T02:00:00Z | Convert tavall-qa test-suite tests to house style |
| [#12](https://github.com/TavallStudios/tavall-qa/pull/12) | `working/wtqa-system-house-tests` | `0c88cc2` | 2026-11-05T01:30:00Z | Tests: convert tavall-qa-system to Tavall house style |

All historical commits up to `9f3655c` are included in `staging/platform-v2`.

## Intended Child PRs (Active Membership)

No active child pull requests currently pending.
