package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.request.TaskBoardAgentExecutionUpdateRequest;
import org.tavall.qa.view.TaskBoardAgentTaskContextView;
import org.tavall.qa.view.TaskBoardAgentTaskView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent-facing role-aware task views and execution update API.
 * Provides authorized task discovery, bounded task-specific context without a workspace dump,
 * and execution updates through owner APIs.
 */
public interface ITaskBoardAgentViewHandler {
    List<TaskBoardAgentTaskView> findAuthorizedAgentTasks(QaSubjectContext subject, Optional<String> projectKey);

    Optional<TaskBoardAgentTaskContextView> findAgentTaskContext(QaSubjectContext subject, UUID taskId);

    TaskBoardTask updateAgentExecution(QaSubjectContext subject, TaskBoardAgentExecutionUpdateRequest request);
}
