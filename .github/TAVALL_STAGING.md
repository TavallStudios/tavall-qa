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

## Active composition

| PR | Branch | Head | State |
|---|---|---|---|
| [#9](https://github.com/TavallStudios/tavall-web-qa/pull/9) | `working/ldr-design-review-webqa` | `3b17b4acf5e59192e50b231e5d61266f7c198de6` | MERGED; gate `ldr-webqa-3b17b4a` / request `01e18ae6-1947-40f5-9a2b-fbbb608acd7c` REQUIRED_ALL PASS; merge `9339a68fc7633b5bcbaf79b6b479f145b00389c6` |

## Historical Reconciliation (`NEEDS_RECONCILIATION`)

These pull requests merged directly into `main` before this root existed:

| PR | Branch | Head SHA | Merged | Description |
|---|---|---|---|---|
| [#6](https://github.com/TavallStudios/tavall-web-qa/pull/6) | working/wui-qa-ventures-20261107 | 3392f98b34a14580048312bb3ec5b9e88d8602bd | 2026-11-07 (merge 8bb1aa6) | Ventures index and per-venture pages; exact-head CI 3d8d528d REQUIRED_ALL PASS |
