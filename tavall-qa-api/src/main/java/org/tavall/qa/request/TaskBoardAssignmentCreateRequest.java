package org.tavall.qa.request;

import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TaskBoardAssignmentCreateRequest(
        UUID taskId,
        UUID accountId,
        QaCapability capability,
        QaEnvironmentGrant environment,
        Optional<QaClass> requiredClass,
        Optional<QaTier> minimumTier,
        String reason
) {
    public TaskBoardAssignmentCreateRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(requiredClass, "requiredClass");
        Objects.requireNonNull(minimumTier, "minimumTier");
        Objects.requireNonNull(reason, "reason");
    }
}
