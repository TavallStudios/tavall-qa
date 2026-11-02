package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.Objects;
import java.util.UUID;

/** Stable external identity and desired task snapshot for authenticated synchronization. */
public record TaskBoardTaskUpsertRequest(
        String externalKey,
        TaskBoardTaskCreateRequest task,
        TaskBoardTaskStatus status,
        UUID existingTaskId
) {
    public TaskBoardTaskUpsertRequest(String externalKey, TaskBoardTaskCreateRequest task, TaskBoardTaskStatus status) {
        this(externalKey, task, status, null);
    }

    public TaskBoardTaskUpsertRequest {
        Objects.requireNonNull(externalKey, "externalKey");
        externalKey = externalKey.trim();
        if (externalKey.isBlank() || externalKey.length() > 255) {
            throw new IllegalArgumentException("externalKey must contain 1..255 characters");
        }
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(status, "status");
    }
}
