# tavall-qa-cli

Typed Java CLI adapter for the canonical QA Task Board. The module extends `tavall-cli-api`
(`CLICommand<T>`, `CLICommandBuilder`, `CLICommandOption`) and resolves every collaborator through
`DependencyAccess<...>`; no constructor-captured collaborators and no secrets in output.

`QaCLIBootstrap` is ServiceLoader-registered through
`META-INF/services/org.tavall.cli.api.bootstrap.ICLIBootstrap`, so the commands are available from the
self-contained artifact (`build/libs/tavall-qa-cli-<version>.jar`) and from the installed
`/usr/local/bin/tavall-qa` wrapper.

## Commands

| Command | Purpose |
| --- | --- |
| `qa task upsert` | Idempotent task upsert by external key. |
| `qa design register` | Register 2–4 live design candidates on a task. |
| `qa design decision` | Read the design review decision attached to a task. |
| `qa github sync` | Read GitHub issues and upsert one QA task per issue. |

## `qa github sync`

Reads issues with `gh api` against the **GitHub REST** issues endpoint
(`repos/<owner>/<repo>/issues?state=<state>&per_page=100&page=<n>`, paginated, never GraphQL),
filters out pull requests, and upserts one task per issue through the typed QA execution client.
Nothing is written back to GitHub.

```text
tavall-qa qa github sync --repos tavall-ai,tavall-docs
tavall-qa qa github sync --owner TavallStudios --repos tavall-ai,tavall-web \
  --state all --limit 25 --project tavall-engineering --section orchestration
tavall-qa qa github sync --repos tavall-ai,tavall-docs --dry-run
```

### Options

| Option | Default | Meaning |
| --- | --- | --- |
| `--owner` | `TavallStudios` | GitHub owner (organization). |
| `--repos` | required | Comma-separated repository names, deduplicated and validated. |
| `--state` | `open` | Issue state filter: `open`, `closed` or `all`. |
| `--dry-run` | `false` | Report the planned keys without calling the QA API (`--dry-run` or `--dry-run=false`). |
| `--limit` | `100` | Maximum issues read per repository. |
| `--project` | `TAVALL_QA_PROJECT` or `tavall-engineering` | QA project key. |
| `--section` | `orchestration` | QA section key. |
| `--credential` | orchestrator agent credential file | Path to the QA agent credential JSON. |

### Deterministic mapping

| Issue | Task |
| --- | --- |
| External key | `github/<owner>/<repo>/issues/<number>` |
| Source type | `github-issue` |
| Source reference | `html_url`, falling back to `https://github.com/<owner>/<repo>/issues/<number>` |
| `state: open` | `READY` |
| `state: closed` + `state_reason: not_planned` | `CANCELLED` |
| any other `closed` | `DONE` |
| `P0` title token, or `p0`/`priority:p0`/`critical` label | `CRITICAL` |
| `P1` title token, or `p1`/`priority:p1`/`high` label | `HIGH` |
| `P3`/`P4` title token, or `low` label | `LOW` |
| everything else | `NORMAL` |
| Description | Sanitized excerpt of the issue body, bounded to 1500 characters, falling back to the title |

The strongest priority signal wins when the title and labels disagree. A dry run reports the exact
`scanned` count and planned `syncedKeys` with `dryRun: true` and makes no QA API call. A per-issue
client failure is recorded in `errors` and counted in `failed` without aborting the remaining
repositories; such a batch exits `1` with failure reason `GITHUB_SYNC_INCOMPLETE`. GitHub reader
failures exit `1` with `GITHUB_READER_ERROR`, and invalid arguments exit `2` with
`INVALID_ARGUMENT`.

## Build and test

```bash
./gradlew :tavall-qa-cli:test
./gradlew :tavall-qa-cli:allJar
java -jar tavall-qa-cli/build/libs/tavall-qa-cli-<version>.jar qa github sync --repos tavall-ai --dry-run
```
