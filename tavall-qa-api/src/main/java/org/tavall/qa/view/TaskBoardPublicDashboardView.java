package org.tavall.qa.view;

import java.util.List;

public record TaskBoardPublicDashboardView(
        boolean storageAvailable,
        List<TaskBoardPublicProjectSummaryView> projects,
        List<TaskBoardPublicProjectView> projectBoards,
        int progressPercent,
        int taskCount,
        int activeCount,
        int completedCount,
        int nhvCount,
        List<TaskBoardPublicActivityView> activeTasks,
        List<TaskBoardPublicActivityView> recentlyCompleted
) {
}
