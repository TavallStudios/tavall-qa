package org.tavall.qa.domain.mirror;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Report produced by verifying mirror state against PostgreSQL authority.
 */
public record TaskBoardFileMirrorVerificationReport(
        Instant inspectedAt,
        List<TaskBoardFileMirrorDrift> drifts
) {
    public TaskBoardFileMirrorVerificationReport {
        Objects.requireNonNull(inspectedAt, "inspectedAt");
        drifts = List.copyOf(Objects.requireNonNull(drifts, "drifts"));
    }

    public boolean hasDrift() {
        return !drifts.isEmpty();
    }

    public int driftCount() {
        return drifts.size();
    }
}
