package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskBoardTaskPageView(
        UUID taskId,
        String projectKey,
        String projectDisplayName,
        String sectionKey,
        String title,
        String summary,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        TaskBoardAudience audience,
        int weight,
        String sourceType,
        Instant updatedAt,
        boolean publicVisible
) {
    public TaskBoardTaskPageView {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(projectDisplayName, "projectDisplayName");
        Objects.requireNonNull(sectionKey, "sectionKey");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(qaStatus, "qaStatus");
        Objects.requireNonNull(audience, "audience");
        sourceType = sourceType == null ? "" : sourceType;
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static TaskBoardTaskPageView publicTask(
            String projectDisplayName,
            TaskBoardPublicTaskView task
    ) {
        return new TaskBoardTaskPageView(
                task.taskId(),
                task.projectKey(),
                projectDisplayName,
                task.sectionKey(),
                task.title(),
                task.summary(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                TaskBoardAudience.PUBLIC,
                task.weight(),
                task.sourceType(),
                task.updatedAt(),
                true
        );
    }

    public static TaskBoardTaskPageView staffTask(TaskBoardTaskView taskView) {
        TaskBoardTask task = taskView.task();
        return new TaskBoardTaskPageView(
                task.taskId(),
                task.projectKey(),
                taskView.project().displayName(),
                task.sectionKey(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                task.audience(),
                task.weight(),
                task.sourceType(),
                task.updatedAt(),
                task.publicVisible()
        );
    }
}
