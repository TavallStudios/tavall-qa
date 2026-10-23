package org.tavall.qa.request;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardAssignmentRevokeRequest(UUID assignmentId, String reason) {
    public TaskBoardAssignmentRevokeRequest {
        Objects.requireNonNull(assignmentId, "assignmentId");
        Objects.requireNonNull(reason, "reason");
    }
}
