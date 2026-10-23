package org.tavall.qa.auth;

import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaSubjectStatus;
import org.tavall.qa.domain.auth.QaTaskAssignmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TaskBoardTestSubjects {
    private TaskBoardTestSubjects() {
    }

    public static QaSubjectContext active(
            UUID accountId,
            UUID minecraftUuid,
            Set<QaClass> classes,
            Optional<QaTier> tier,
            Set<QaEnvironmentGrant> environments,
            Set<QaCapability> capabilities,
            List<QaTaskAssignmentGrant> assignments,
            boolean technicalAgreement,
            boolean globalSuperuser
    ) {
        return new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectStatus.ACTIVE,
                Optional.ofNullable(minecraftUuid),
                Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000701")),
                true,
                globalSuperuser,
                classes,
                tier,
                environments,
                capabilities,
                assignments,
                technicalAgreement
        );
    }

    public static QaTaskAssignmentGrant assignment(UUID taskId, QaCapability capability, QaEnvironmentGrant environment) {
        return new QaTaskAssignmentGrant(
                UUID.fromString("00000000-0000-0000-0000-000000000701"),
                taskId,
                capability,
                environment,
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-10-23T00:00:00Z")
        );
    }

    public static QaSubjectContext manager(UUID accountId, UUID minecraftUuid) {
        return active(
                accountId,
                minecraftUuid,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD),
                Set.of(
                        QaCapability.VIEW_QA_TASKS,
                        QaCapability.VIEW_STAFF_TASKS,
                        QaCapability.VIEW_STAFF_WORKSPACE,
                        QaCapability.SUBMIT_EVIDENCE,
                        QaCapability.REVIEW_EVIDENCE,
                        QaCapability.ADD_COMMENT,
                        QaCapability.ASSIGN_OTHERS,
                        QaCapability.ACCEPT_REJECT_TASK,
                        QaCapability.HUMAN_VERIFY,
                        QaCapability.MANAGE_TASKS,
                        QaCapability.MANAGE_PROJECTS,
                        QaCapability.MANAGE_QA_MESSAGES
                ),
                List.of(),
                false,
                false
        );
    }

    public static QaSubjectContext assigned(
            UUID accountId,
            UUID minecraftUuid,
            QaCapability capability,
            UUID taskId,
            QaEnvironmentGrant environment
    ) {
        return active(
                accountId,
                minecraftUuid,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_III),
                Set.of(environment),
                Set.of(capability),
                List.of(assignment(taskId, capability, environment)),
                false,
                false
        );
    }
}
