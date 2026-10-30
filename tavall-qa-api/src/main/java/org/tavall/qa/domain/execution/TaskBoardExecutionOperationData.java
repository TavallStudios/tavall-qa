package org.tavall.qa.domain.execution;

import java.util.List;
import java.util.Objects;

/** Completed or pending bounded work and the receipts needed to verify it. */
public record TaskBoardExecutionOperationData(String operationId, String description,
        TaskBoardExecutionStatus status, List<TaskBoardExecutionReferenceData> receipts) {
    public TaskBoardExecutionOperationData {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(status, "status");
        receipts = List.copyOf(receipts);
        if (operationId.isBlank() || operationId.length() > 256 || description.isBlank()
                || description.length() > 4096 || receipts.size() > 128) {
            throw new IllegalArgumentException("Checkpoint operations must be bounded and explicitly identified");
        }
    }
}
