package org.tavall.qa.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Current task-scoped capability assignment resolved from the QA authority. */
public record QaTaskAssignmentGrant(
        UUID organizationId,
        UUID taskId,
        QaCapability capability,
        QaEnvironmentGrant environment,
        Optional<QaClass> requiredClass,
        Optional<QaTier> minimumTier,
        Instant assignedAt
) {
    public QaTaskAssignmentGrant {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(requiredClass, "requiredClass");
        Objects.requireNonNull(minimumTier, "minimumTier");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
