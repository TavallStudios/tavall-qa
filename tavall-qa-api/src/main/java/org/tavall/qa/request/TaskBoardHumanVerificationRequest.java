package org.tavall.qa.request;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardHumanVerificationRequest(
        UUID taskId,
        UUID evidenceId,
        QaEnvironmentGrant environment
) {
    public TaskBoardHumanVerificationRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(environment, "environment");
    }
}
