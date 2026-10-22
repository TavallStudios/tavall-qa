package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.request.TaskBoardActionCreateRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.time.Instant;
import java.util.UUID;

public interface ITaskBoardMutationHandler {
    TaskBoardProject createProject(TaskBoardProjectCreateRequest request);

    TaskBoardTask createTask(TaskBoardTaskCreateRequest request);

    TaskBoardTask updateTask(TaskBoardTaskUpdateRequest request);

    TaskBoardTask verifyTask(UUID taskId, UUID verifierMinecraftUuid, Instant verifiedAt);

    TaskBoardTask rejectTask(UUID taskId, String reason);

    TaskBoardAction addAction(TaskBoardActionCreateRequest request);

    TaskBoardComment addComment(TaskBoardCommentCreateRequest request);
}
