package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAuditEntry;

import java.util.Objects;

/** Staff-only audit projection. Never included in the public task response. */
public record TaskBoardAuditView(TaskBoardAuditEntry event) {
    public TaskBoardAuditView {
        Objects.requireNonNull(event, "event");
    }
}
