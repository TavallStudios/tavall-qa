package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Request-specific, server-resolved controls for the Focus Workspace presentation. */
public record QaWorkspacePageContext(
        String activeFocus,
        String statePreview,
        String csrfParameterName,
        String csrfToken,
        Optional<UUID> organizationId,
        Set<QaEnvironmentGrant> evidenceEnvironments,
        Set<QaEnvironmentGrant> reviewEnvironments,
        Set<QaEnvironmentGrant> acceptanceEnvironments,
        Set<QaEnvironmentGrant> commentEnvironments,
        Set<UUID> verifiableEvidenceIds,
        boolean linkedMinecraftIdentity,
        boolean canManageTask,
        boolean canAssignTask
) {
    public QaWorkspacePageContext {
        activeFocus = activeFocus == null ? "OVERVIEW" : activeFocus;
        statePreview = statePreview == null ? "CURRENT" : statePreview;
        csrfParameterName = csrfParameterName == null || csrfParameterName.isBlank() ? "_csrf" : csrfParameterName;
        csrfToken = csrfToken == null ? "" : csrfToken;
        Objects.requireNonNull(organizationId, "organizationId");
        evidenceEnvironments = Set.copyOf(Objects.requireNonNull(evidenceEnvironments, "evidenceEnvironments"));
        reviewEnvironments = Set.copyOf(Objects.requireNonNull(reviewEnvironments, "reviewEnvironments"));
        acceptanceEnvironments = Set.copyOf(Objects.requireNonNull(acceptanceEnvironments, "acceptanceEnvironments"));
        commentEnvironments = Set.copyOf(Objects.requireNonNull(commentEnvironments, "commentEnvironments"));
        verifiableEvidenceIds = Set.copyOf(Objects.requireNonNull(verifiableEvidenceIds, "verifiableEvidenceIds"));
    }
}
