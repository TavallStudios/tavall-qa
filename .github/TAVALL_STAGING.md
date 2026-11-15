# Tavall Web QA Staging Root

```text
<!-- tavall-staging:v1 -->
Type: RUNTIME
State: ACTIVE
Branch: staging/runtime-qa-web
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/runtime-qa-web
```

This branch is the persistent runtime integration root for `tavall-web-qa`: the QA and Task Board pages that Tavall Web consumes through its `tavall-web-qa` source pin. Feature, fix and design PRs target `staging/runtime-qa-web`. Promotion to `main` goes through the Draft staging PR. A consumer pin bump in Tavall Web pins the promoted `main` commit.

Established 2026-11-07 by wUI at `main` 8bb1aa6. The repo had no staging root.

Refreshed 2026-11-15 by qahouse-20261115: the previous root PR [#7](https://github.com/TavallStudios/tavall-web-qa/pull/7) was already merged into `main` at `197dabca4287572df0c3af66e44b7c0fc6be77ab`, leaving no active Draft staging PR. The branch was fast-forwarded to that `main` head and a replacement Draft staging PR to `main` was opened in the same operation (tracked from [issue #5](https://github.com/TavallStudios/tavall-web-qa/issues/5)). Focused PRs continue to target this branch.

## Active composition

| PR | Branch | Head | State |
|---|---|---|---|
| [#8](https://github.com/TavallStudios/tavall-web-qa/pull/8) | `working/w18-house-tests-qa-20261107` | `41e955e0` | MERGED; gate `w18-house-tests-qa2` / request `41e955e0-4f2d-481c-a953-d8f3e0e2aed7` REQUIRED_ALL PASS; merge `9f0a5955b2806e85ada7775b82f11cdc71e59e14` |
| [#9](https://github.com/TavallStudios/tavall-web-qa/pull/9) | `working/ldr-design-review-webqa` | `3b17b4acf5e59192e50b231e5d61266f7c198de6` | MERGED; gate `ldr-webqa-3b17b4a` / request `01e18ae6-1947-40f5-9a2b-fbbb608acd7c` REQUIRED_ALL PASS; merge `9339a68fc7633b5bcbaf79b6b479f145b00389c6` |
| [#10](https://github.com/TavallStudios/tavall-web-qa/pull/10) | `working/ldr-webqa-root-refresh` | `9723bbc` | MERGED; docs-only staging manifest refresh, no separate gate; merge `3ea09b19c3b4a8191985bfe753d95b554d7818db` |
| [#12](https://github.com/TavallStudios/tavall-web-qa/pull/12) | `working/qahouse-20261115-qa-page-tests` | `20dc0d4` | MERGED; gate `qahouse-20261115-20dc0d4` / request `a0fa5028-9394-42fc-ad6d-0c34c94548ca` REQUIRED_ALL PASS; merge `51e79b09abea98b9dcde1b1e699de0450103d63d` |

## Historical Reconciliation (`NEEDS_RECONCILIATION`)

These pull requests merged directly into `main` before this root existed:

| PR | Branch | Head SHA | Merged | Description |
|---|---|---|---|---|
| [#6](https://github.com/TavallStudios/tavall-web-qa/pull/6) | working/wui-qa-ventures-20261107 | 3392f98b34a14580048312bb3ec5b9e88d8602bd | 2026-11-07 (merge 8bb1aa6) | Ventures index and per-venture pages; exact-head CI 3d8d528d REQUIRED_ALL PASS |
