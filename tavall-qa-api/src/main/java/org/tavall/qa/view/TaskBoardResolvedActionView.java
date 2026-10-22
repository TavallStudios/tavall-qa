package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAction;

public record TaskBoardResolvedActionView(
        TaskBoardAction action,
        String href
) {
}
