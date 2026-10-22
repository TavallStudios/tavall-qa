package org.tavall.qa.view;

import java.util.List;

public record TaskBoardPublicProjectView(
        TaskBoardPublicProjectSummaryView summary,
        List<TaskBoardPublicSectionView> sections,
        List<TaskBoardPublicTaskView> highlights
) {
}
