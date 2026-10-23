package org.tavall.qa.request;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardTaskAcceptanceRequest(
        UUID taskId,
        UUID evidenceId,
        QaEnvironmentGrant environment
) {
    public TaskBoardTaskAcceptanceRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(environment, "environment");
    }
}
