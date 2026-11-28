package org.tavall.qa.cli.client;

import java.util.Objects;
import java.util.UUID;

/** Typed result of a provisional AI design review recording. */
public record QaCliDesignAiReviewResult(
        UUID taskId,
        int revision,
        String aiReviewerModel,
        String aiCandidateKey,
        boolean decided,
        boolean deployable
) {
    public QaCliDesignAiReviewResult {
        Objects.requireNonNull(taskId, "taskId");
    }
}
