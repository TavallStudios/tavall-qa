package org.tavall.qa.domain.mirror;

import java.util.Objects;

/**
 * An individual detected drift or discrepancy item.
 */
public record TaskBoardFileMirrorDrift(
        String recordType,
        String recordId,
        TaskBoardFileMirrorDriftType driftType,
        String details
) {
    public TaskBoardFileMirrorDrift {
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(driftType, "driftType");
        Objects.requireNonNull(details, "details");
    }
}
