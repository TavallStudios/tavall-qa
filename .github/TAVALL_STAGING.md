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
| (none yet) | | | |

## Historical Reconciliation (`NEEDS_RECONCILIATION`)

These pull requests merged directly into `main` before this root existed:

| PR | Branch | Head SHA | Merged | Description |
|---|---|---|---|---|
| [#6](https://github.com/TavallStudios/tavall-web-qa/pull/6) | working/wui-qa-ventures-20261107 | 3392f98b34a14580048312bb3ec5b9e88d8602bd | 2026-11-07 (merge 8bb1aa6) | Ventures index and per-venture pages; exact-head CI 3d8d528d REQUIRED_ALL PASS |
