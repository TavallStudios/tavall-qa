package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardProjectStatus;

public record TaskBoardPublicProjectSummaryView(
        String projectKey,
        String displayName,
        String description,
        TaskBoardProjectStatus status,
        int progressPercent,
        int taskCount,
        int activeCount,
        int completedCount,
        int nhvCount,
        int sectionCount
) {
}
