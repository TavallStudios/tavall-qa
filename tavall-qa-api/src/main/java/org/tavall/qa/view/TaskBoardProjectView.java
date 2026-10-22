package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardProject;

import java.util.List;

public record TaskBoardProjectView(
        TaskBoardProject project,
        List<TaskBoardColumnView> columns,
        int taskCount,
        int nhvCount,
        int hostRequiredCount,
        int blockedCount,
        int verifiedCount
) {
}
