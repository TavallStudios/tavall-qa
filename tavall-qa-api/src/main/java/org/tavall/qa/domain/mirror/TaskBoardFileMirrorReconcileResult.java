package org.tavall.qa.domain.mirror;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of an explicit typed reconcile operation synchronizing the mirror from PostgreSQL.
 */
public record TaskBoardFileMirrorReconcileResult(
        Instant reconciledAt,
        int tasksMirrored,
        int leasesMirrored,
        int checkpointsMirrored,
        int driftsResolved,
        int quarantinedFiles
) {
    public TaskBoardFileMirrorReconcileResult {
        Objects.requireNonNull(reconciledAt, "reconciledAt");
    }
}
