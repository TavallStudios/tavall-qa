package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.List;

public record TaskBoardColumnView(
        TaskBoardTaskStatus status,
        List<TaskBoardTask> tasks
) {
}
