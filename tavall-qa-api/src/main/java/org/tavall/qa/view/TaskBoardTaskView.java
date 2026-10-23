package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;

import java.util.List;

public record TaskBoardTaskView(
        TaskBoardProject project,
        TaskBoardTask task,
        List<TaskBoardResolvedActionView> actions,
        List<TaskBoardComment> comments,
        List<TaskBoardEvidenceView> evidence,
        List<TaskBoardAssignmentView> assignments,
        List<TaskBoardAuditView> history
) {
    public TaskBoardTaskView {
        actions = List.copyOf(actions);
        comments = List.copyOf(comments);
        evidence = List.copyOf(evidence);
        assignments = List.copyOf(assignments);
        history = List.copyOf(history);
    }
}
