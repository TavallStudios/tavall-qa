package org.tavall.qa.view;

import java.util.List;

public record TaskBoardPublicSectionView(
        String sectionKey,
        int progressPercent,
        int completedWeight,
        int totalWeight,
        int taskCount,
        int activeCount,
        int completedCount,
        int nhvCount,
        List<TaskBoardPublicTaskView> tasks
) {
}
