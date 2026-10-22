package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;

import java.util.List;

public record TaskBoardTaskView(
        TaskBoardProject project,
        TaskBoardTask task,
        List<TaskBoardResolvedActionView> actions,
        List<TaskBoardComment> comments
) {
}
