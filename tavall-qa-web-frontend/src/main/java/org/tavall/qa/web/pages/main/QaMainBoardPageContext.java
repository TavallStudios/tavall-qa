package org.tavall.qa.web.pages.main;

import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardQaProgressionView;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Request-specific account, assignment, and CSRF context for the public board. */
public record QaMainBoardPageContext(
        String identityStatus,
        Optional<UUID> organizationId,
        String csrfParameterName,
        String csrfToken,
        boolean canManageProjects,
        Optional<TaskBoardQaProgressionView> qaProgression,
        List<TaskBoardTaskPageView> assignedTasks
) {
    public QaMainBoardPageContext {
        identityStatus = identityStatus == null ? "ANONYMOUS" : identityStatus;
        Objects.requireNonNull(organizationId, "organizationId");
        csrfParameterName = csrfParameterName == null || csrfParameterName.isBlank() ? "_csrf" : csrfParameterName;
        csrfToken = csrfToken == null ? "" : csrfToken;
        Objects.requireNonNull(qaProgression, "qaProgression");
        assignedTasks = List.copyOf(Objects.requireNonNull(assignedTasks, "assignedTasks"));
    }

    public static QaMainBoardPageContext anonymous() {
        return new QaMainBoardPageContext("ANONYMOUS", Optional.empty(), "_csrf", "", false, Optional.empty(), List.of());
    }
}
