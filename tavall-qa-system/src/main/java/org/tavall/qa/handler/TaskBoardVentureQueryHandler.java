package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardProjectKey;
import org.tavall.qa.domain.TaskBoardRepositoryIdentity;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardVentureDataHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TaskBoardVentureQueryHandler implements ITaskBoardVentureQueryHandler {
    private final ITaskBoardVentureDataHandler dataHandler;
    private final ITaskBoardDataHandler taskDataHandler;

    public TaskBoardVentureQueryHandler(
            ITaskBoardVentureDataHandler dataHandler,
            ITaskBoardDataHandler taskDataHandler
    ) {
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.taskDataHandler = Objects.requireNonNull(taskDataHandler, "taskDataHandler");
    }

    @Override
    public List<TaskBoardVenture> findVentures(QaSubjectContext subject) {
        requireVentureAccess(subject);
        return dataHandler.findVentures().stream()
                .sorted(Comparator.comparing(TaskBoardVenture::slug))
                .toList();
    }

    @Override
    public Optional<TaskBoardVenture> findVenture(String slug, QaSubjectContext subject) {
        requireVentureAccess(subject);
        String key = Objects.requireNonNull(slug, "slug").trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        return dataHandler.findVenture(key);
    }

    @Override
    public List<TaskBoardTask> findVentureTasks(String slug, QaSubjectContext subject) {
        requireVentureAccess(subject);
        TaskBoardVenture venture = findVenture(slug, subject)
                .orElseThrow(() -> new IllegalArgumentException("Unknown venture: " + slug));
        return taskDataHandler.findTasks(TaskBoardProjectKey.ENGINEERING.value()).stream()
                .filter(task -> TaskBoardRepositoryIdentity.fromSourceReference(task.sourceReference())
                        .map(venture::owns)
                        .orElse(false))
                .sorted(Comparator.comparing(TaskBoardTask::updatedAt).reversed())
                .toList();
    }

    private static void requireVentureAccess(QaSubjectContext subject) {
        Objects.requireNonNull(subject, "subject");
        if (!subject.canManageProject()) {
            throw new SecurityException("Venture directory requires project-management access");
        }
    }
}
