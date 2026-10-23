package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.UUID;

public record TaskBoardCommentCreateRequest(
        UUID taskId,
        UUID parentCommentId,
        TaskBoardCommentType type,
        String body,
        QaEnvironmentGrant environment
) {
}
