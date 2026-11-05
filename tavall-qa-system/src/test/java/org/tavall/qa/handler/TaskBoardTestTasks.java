package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class TaskBoardTestTasks {
    private static final AtomicLong IDS = new AtomicLong(100);
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    private TaskBoardTestTasks() { }

    static TaskBoardTask task(TaskBoardTaskStatus status, int weight) {
        return new TaskBoardTask(new UUID(0, IDS.incrementAndGet()), "project-test", "section-test",
                "Test task", "Public test task", "Description", "Summary", status,
                TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, weight,
                false, "manual", "qa://task", true, 1, new UUID(0, 1), null, null, null, null, null,
                1, NOW, NOW);
    }
}
