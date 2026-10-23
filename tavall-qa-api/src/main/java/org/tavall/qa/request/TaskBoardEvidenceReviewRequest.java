package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardEvidenceReviewRequest(
        UUID evidenceId,
        TaskBoardEvidenceStatus result,
        QaEnvironmentGrant environment,
        boolean publicSummaryApproved,
        String reason
) {
    public TaskBoardEvidenceReviewRequest {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(environment, "environment");
        reason = reason == null ? "" : reason;
        if (result != TaskBoardEvidenceStatus.ACCEPTED && publicSummaryApproved) {
            throw new IllegalArgumentException("Only accepted evidence can publish a public summary");
        }
    }
}
