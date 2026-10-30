package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.notion.NotionTaskSyncState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Task-specific context for an agent without performing a workspace or project dump.
 */
public record TaskBoardAgentTaskContextView(
        TaskBoardTask task,
        Optional<NotionTaskSyncState> syncState,
        List<TaskBoardAction> actions,
        List<TaskBoardComment> comments,
        List<TaskBoardEvidence> evidence,
        List<TaskBoardAssignment> assignments,
        Map<String, String> planningContext
) {
    public TaskBoardAgentTaskContextView {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(syncState, "syncState");
        actions = actions == null ? List.of() : List.copyOf(actions);
        comments = comments == null ? List.of() : List.copyOf(comments);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
        planningContext = planningContext == null ? Map.of() : Map.copyOf(planningContext);
    }
}
