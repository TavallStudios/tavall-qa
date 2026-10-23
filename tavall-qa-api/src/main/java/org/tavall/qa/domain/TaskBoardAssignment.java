package org.tavall.qa.domain;

import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTaskAssignmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TaskBoardAssignment(
        UUID assignmentId,
        UUID organizationId,
        UUID taskId,
        UUID accountId,
        QaCapability capability,
        QaEnvironmentGrant environment,
        Optional<QaClass> requiredClass,
        Optional<QaTier> minimumTier,
        UUID assignedByAccountId,
        String reason,
        Instant assignedAt,
        Instant revokedAt
) {
    public TaskBoardAssignment {
        Objects.requireNonNull(assignmentId, "assignmentId");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(requiredClass, "requiredClass");
        Objects.requireNonNull(minimumTier, "minimumTier");
        Objects.requireNonNull(assignedByAccountId, "assignedByAccountId");
        Objects.requireNonNull(reason, "reason");
        reason = reason.trim();
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Assignment reason must not be blank");
        }
        Objects.requireNonNull(assignedAt, "assignedAt");
        if (revokedAt != null && revokedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("Assignment revocation cannot predate assignment");
        }
    }

    public boolean activeAt(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return revokedAt == null || instant.isBefore(revokedAt);
    }

    public QaTaskAssignmentGrant toGrant() {
        return new QaTaskAssignmentGrant(
                organizationId,
                taskId,
                capability,
                environment,
                requiredClass,
                minimumTier,
                assignedAt
        );
    }

    public TaskBoardAssignment revoke(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        if (revokedAt != null) {
            return this;
        }
        return new TaskBoardAssignment(
                assignmentId,
                organizationId,
                taskId,
                accountId,
                capability,
                environment,
                requiredClass,
                minimumTier,
                assignedByAccountId,
                reason,
                assignedAt,
                instant
        );
    }
}
