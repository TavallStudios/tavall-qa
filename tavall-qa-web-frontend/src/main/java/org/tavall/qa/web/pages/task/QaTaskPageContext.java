package org.tavall.qa.web.pages.task;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Request-specific authorization and CSRF data for the safe task-page forms. */
public record QaTaskPageContext(
        String csrfParameterName,
        String csrfToken,
        Optional<UUID> organizationId,
        Set<UUID> verifiableEvidenceIds,
        Set<QaEnvironmentGrant> evidenceEnvironments,
        Set<QaEnvironmentGrant> reviewEnvironments,
        Set<QaEnvironmentGrant> acceptanceEnvironments,
        Set<QaEnvironmentGrant> commentEnvironments,
        Optional<QaTaskExecutionView> execution
) {
    public QaTaskPageContext {
        csrfParameterName = csrfParameterName == null || csrfParameterName.isBlank() ? "_csrf" : csrfParameterName;
        csrfToken = csrfToken == null ? "" : csrfToken;
        Objects.requireNonNull(organizationId, "organizationId");
        verifiableEvidenceIds = Set.copyOf(Objects.requireNonNull(verifiableEvidenceIds, "verifiableEvidenceIds"));
        evidenceEnvironments = Set.copyOf(Objects.requireNonNull(evidenceEnvironments, "evidenceEnvironments"));
        reviewEnvironments = Set.copyOf(Objects.requireNonNull(reviewEnvironments, "reviewEnvironments"));
        acceptanceEnvironments = Set.copyOf(Objects.requireNonNull(acceptanceEnvironments, "acceptanceEnvironments"));
        commentEnvironments = Set.copyOf(Objects.requireNonNull(commentEnvironments, "commentEnvironments"));
        execution = execution == null ? Optional.empty() : execution;
    }

    public QaTaskPageContext(
            String csrfParameterName,
            String csrfToken,
            Optional<UUID> organizationId,
            Set<UUID> verifiableEvidenceIds,
            Set<QaEnvironmentGrant> evidenceEnvironments,
            Set<QaEnvironmentGrant> reviewEnvironments,
            Set<QaEnvironmentGrant> acceptanceEnvironments,
            Set<QaEnvironmentGrant> commentEnvironments
    ) {
        this(csrfParameterName, csrfToken, organizationId, verifiableEvidenceIds, evidenceEnvironments,
                reviewEnvironments, acceptanceEnvironments, commentEnvironments, Optional.empty());
    }

    public static QaTaskPageContext readOnly() {
        return new QaTaskPageContext("_csrf", "", Optional.empty(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Optional.empty());
    }

    public QaTaskPageContext withExecution(QaTaskExecutionView executionView) {
        return new QaTaskPageContext(
                csrfParameterName,
                csrfToken,
                organizationId,
                verifiableEvidenceIds,
                evidenceEnvironments,
                reviewEnvironments,
                acceptanceEnvironments,
                commentEnvironments,
                Optional.ofNullable(executionView)
        );
    }
}
