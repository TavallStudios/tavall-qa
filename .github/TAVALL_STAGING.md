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

The successor Draft root remains the path for future migration/deployment follow-up; its documentation-only source has no new runtime acceptance claim or standalone feature gate.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-qa/.github/TAVALL_STAGING.md` | 2026-11-13 7:33 AM PST | QA #24 merged at cd74c54; successor staging checkpoint. |
| Notion | `NOT_APPLICABLE` | — | 2026-11-13 7:33 AM PST | Git integration topology manifest; GitHub PRs remain authoritative. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-13 7:33 AM PST | GitHub | `UPDATED` | `.github/TAVALL_STAGING.md` | Same path | QA #24 exact-source gate and merge | Refreshed successor Generation 4 and recorded source acceptance; earlier history retained. |

</details>
