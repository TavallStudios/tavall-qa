package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAssignment;

import java.util.Objects;

/** Staff-only projection of an assignment and its revocation history. */
public record TaskBoardAssignmentView(TaskBoardAssignment assignment) {
    public TaskBoardAssignmentView {
        Objects.requireNonNull(assignment, "assignment");
    }
}
