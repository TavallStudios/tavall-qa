package org.tavall.qa.view;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed planning identity an agent needs to act on one task. {@code taskFingerprint} is the value an execution lease
 * request must echo as {@code expectedTaskFingerprint}. Notion mapping state lives in the view's sync state.
 */
public record TaskBoardAgentPlanningContext(
        UUID taskId,
        String projectKey,
        int qaRevision,
        String sourceType,
        String sourceReference,
        String taskFingerprint
) {
    public TaskBoardAgentPlanningContext {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        sourceType = sourceType == null ? "" : sourceType;
        sourceReference = sourceReference == null ? "" : sourceReference;
        Objects.requireNonNull(taskFingerprint, "taskFingerprint");
    }
}
