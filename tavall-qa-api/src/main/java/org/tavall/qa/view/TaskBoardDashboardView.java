package org.tavall.qa.view;

import java.util.List;

public record TaskBoardDashboardView(
        boolean storageAvailable,
        List<TaskBoardProjectSummaryView> projects
) {
}
