# Tavall QA Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform-v2
Parent: main
Promotion: EXACT_HEAD_REQUIRED_ALL
ChildMergeTarget: staging/platform-v2
```

This branch is the persistent repository-level integration root for Tavall QA (Generation 4). Normal QA pull requests target `staging/platform-v2`. Pull requests remain the work and review records; this manifest tracks active composition and historical reconciliation.

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

## Promoted Generation 3 Design Review Work for Issue #23

The generic design-review API changes for [issue #23](https://github.com/TavallStudios/tavall-qa/issues/23) are integrated into this staging root:

| PR | Source SHA | Exact-head CI request | Merge SHA | Status |
|---|---|---|---|---|
| [#26](https://github.com/TavallStudios/tavall-qa/pull/26) | `1094b3737ca45b7ee476454c59825d111dd20fcc` | `ea5f8ec4-6810-4389-8104-cafd4132920c` (REQUIRED_ALL PASS) | `9acf61946a55c98151e00a964a2789a3680c8e34` | Merged |
| [#27](https://github.com/TavallStudios/tavall-qa/pull/27) | `87dabd58ed4e39f5b7c283a6a9ed0eb8b01b8a63` | `12206f0d-cf49-4822-bd24-b1685b8280ff` (REQUIRED_ALL PASS) | `c363402785e6c821d9c477a10f68175301703c0d` | Merged |

Generation 3 retains ancestry from main `fcbe2563b6fb018f3379a260a6ec3747d378d631`, including PRs #18, #19 and #20. No source was removed or rewritten. Exact composed source `36af58d2e7200c80b2a06ca8cd47a1f317bb61ef` passed COMPILATION, UNIT_TESTS, INTEGRATION, QUALITY and REQUIRED_ALL in request `9174c660-f796-4353-9b3f-85a8e42a858d`. Root PR #24 promoted it to main as `cd74c548476232f37584fbc12d2091d8ef0a934c`. The QA library has no independent runtime; the Web consumer has separate Development and Staging delivery receipts. Live migration 041 and owner-ready Production acceptance remain blocked.

## Generation 4 opening checkpoint

The prior `staging/platform-v2` source `36af58d2e7200c80b2a06ca8cd47a1f317bb61ef` is a strict ancestor of current main `cd74c548476232f37584fbc12d2091d8ef0a934c`. This successor root fast-forwards the persistent staging line to that main baseline and refreshes only documentation. No active focused QA children were present when PR #24 merged. Legacy root #4 is explicitly SUPERSEDED by #18 and is not the active integration authority.

Root [PR #29](https://github.com/TavallStudios/tavall-qa/pull/29) was MERGED to `main` at `51c9d3ed5d607a77d337c14efb8ed8f8db741760` on exact head `785b8adf32d24f97099be8c52db5fee5d3c72bf8` (request `8a0643ef-d256-4f41-a737-51369ba5bbf9`, REQUIRED_ALL PASS). Its documentation-only source introduced no runtime acceptance claim.

## Generation 5 opening checkpoint

`staging/platform-v2` was a strict ancestor of `main` when no active staging child PR remained. This checkpoint fast-forwards the persistent staging line to main `c4fb7844cc0d139ed9506b1ecab46f49c81d81bb`, which includes merged [PR #30](https://github.com/TavallStudios/tavall-qa/pull/30) (progression) and [PR #31](https://github.com/TavallStudios/tavall-qa/pull/31) (migration-041 persistence-contract tests, exact head `499f8ee4f1b60338a17963012a179370e2717a7a`, request `44fcc365-08ab-49e7-9d47-df7f88ce7869`, REQUIRED_ALL PASS). No source was removed or rewritten.

Migration `041_task_board_design_review.sql` is now applied out-of-band to DEVELOPMENT `novus_web` and PRODUCTION `tavall_web_production` through the owner-documented path (backups under `/srv/storage-0/backups/novus_web/`; table, 10 columns, three CHECK constraints and DML grants verified; candidate A/B/C → winner C round-trip committed then rolled back). Deployments do not apply DDL (`generateSchema` false). The remaining owner action is a single authenticated browser sign-in at `http://localhost:19081` and winner selection; the public QA endpoint still redirects anonymously (301/302). This documentation-only root refresh adds no new runtime acceptance claim.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-qa/.github/TAVALL_STAGING.md` | 2026-11-15 15:20 UTC | Generation 5 fast-forward to main c4fb784; migration 041 applied/verified in DEV+PROD. |
| Notion | `NOT_APPLICABLE` | — | 2026-11-15 15:20 UTC | Git integration topology manifest; GitHub PRs remain authoritative. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 7:33 AM PST | GitHub | `UPDATED` | `.github/TAVALL_STAGING.md` | Same path | QA #24 exact-source gate and merge | Refreshed successor Generation 4 and recorded source acceptance; earlier history retained. |
| 2026-11-15 15:20 UTC | GitHub | `UPDATED` | `.github/TAVALL_STAGING.md` | Same path | QA #29/#30/#31 merges; migration 041 applied/verified | Opened Generation 5 from strict-ancestor fast-forward; recorded out-of-band migration application and single owner sign-in step; earlier history retained. |

</details>
