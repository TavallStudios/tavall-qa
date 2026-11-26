package org.tavall.qa.cli.github;

import java.util.List;

/**
 * Typed outcome of {@code qa github sync}.
 *
 * <p>For a completed batch {@code scanned == created + updated + skipped + failed}; a dry run
 * reports the planned {@link #syncedKeys()} with {@code created}, {@code updated}, {@code skipped}
 * and {@code failed} all zero because no QA API call is made.</p>
 */
public record QaGithubSyncResult(
        String owner,
        List<String> repositories,
        String stateFilter,
        boolean dryRun,
        int scanned,
        int created,
        int updated,
        int skipped,
        int failed,
        List<String> syncedKeys,
        List<String> errors) {

    public QaGithubSyncResult {
        owner = owner == null ? "" : owner;
        repositories = repositories == null ? List.of() : List.copyOf(repositories);
        stateFilter = stateFilter == null ? "" : stateFilter;
        requireCount(scanned, "scanned");
        requireCount(created, "created");
        requireCount(updated, "updated");
        requireCount(skipped, "skipped");
        requireCount(failed, "failed");
        syncedKeys = syncedKeys == null ? List.of() : List.copyOf(syncedKeys);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    /** True when no issue failed to sync. */
    public boolean successful() {
        return failed == 0;
    }

    /** Number of tasks created or refreshed by a non-dry-run batch. */
    public int synced() {
        return created + updated;
    }

    private static void requireCount(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
