package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardProject;

public record TaskBoardProjectSummaryView(
        TaskBoardProject project,
        int taskCount,
        int activeCount,
        int blockedCount,
        int nhvCount,
        int verifiedCount
) {
}
