package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardEvidence;

import java.util.Objects;

/** Staff-only evidence projection including private review context. */
public record TaskBoardEvidenceView(TaskBoardEvidence evidence) {
    public TaskBoardEvidenceView {
        Objects.requireNonNull(evidence, "evidence");
    }
}
