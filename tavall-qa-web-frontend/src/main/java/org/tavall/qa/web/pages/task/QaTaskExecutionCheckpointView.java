package org.tavall.qa.web.pages.task;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** View projection of one ordered task execution checkpoint. */
public record QaTaskExecutionCheckpointView(
        UUID checkpointId,
        long sequence,
        String providerId,
        String status,
        Instant recordedAt,
        String nextAction
) {
    public QaTaskExecutionCheckpointView {
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(recordedAt, "recordedAt");
        nextAction = nextAction == null ? "" : nextAction;
    }
}
