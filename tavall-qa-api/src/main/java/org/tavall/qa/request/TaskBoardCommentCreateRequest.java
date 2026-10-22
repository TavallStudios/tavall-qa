package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardCommentType;

import java.util.UUID;

public record TaskBoardCommentCreateRequest(
        UUID taskId,
        UUID parentCommentId,
        UUID authorMinecraftUUID,
        TaskBoardCommentType type,
        String body
) {
}
