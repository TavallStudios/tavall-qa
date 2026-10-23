package org.tavall.qa.auth;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardAuthTest {
    private final UUID accountId = UUID.randomUUID();
    private final UUID minecraftUuid = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @Test
    void anonymousAndOrdinarySignedInAccountsOnlySeePublicAudience() {
        QaSubjectContext anonymous = QaSubjectContext.anonymous();
        QaSubjectContext ordinary = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectStatus.ACTIVE,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                false
        );

        assertTrue(anonymous.isAnonymous());
        assertTrue(anonymous.canViewAudience(TaskBoardAudience.PUBLIC, taskId));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.QA_TESTER, taskId));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.STAFF, taskId));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.ADMIN_ONLY, taskId));
        assertFalse(ordinary.isAnonymous());
        assertTrue(ordinary.canViewAudience(TaskBoardAudience.PUBLIC, taskId));
        assertFalse(ordinary.canViewAudience(TaskBoardAudience.QA_TESTER, taskId));
        assertFalse(ordinary.canViewStaffWorkspace());
    }

    @Test
    void qaTierDoesNotGrantAudienceOrCapabilitiesByItself() {
        QaSubjectContext senior = TaskBoardTestSubjects.active(
                accountId,
                null,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD),
                Set.of(),
                List.of(),
                false,
                false
        );

        assertTrue(senior.canViewAudience(TaskBoardAudience.PUBLIC, taskId));
        assertFalse(senior.canViewAudience(TaskBoardAudience.QA_TESTER, taskId));
        assertFalse(senior.canViewAudience(TaskBoardAudience.STAFF, taskId));
        assertFalse(senior.canManageProject());
        assertFalse(senior.canManageTasks());
        assertFalse(senior.canAssignTask());
    }

    @Test
    void foundingPublicTesterNeedsExplicitPublicParticipationCapabilityAndAssignment() {
        QaTaskAssignmentGrant assignment = TaskBoardTestSubjects.assignment(
                taskId,
                QaCapability.SUBMIT_EVIDENCE,
                QaEnvironmentGrant.DEV
        );
        QaSubjectContext foundingTester = TaskBoardTestSubjects.active(
                accountId,
                null,
                Set.of(QaClass.FOUNDING_PUBLIC),
                Optional.empty(),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.PARTICIPATE_PUBLIC_QA, QaCapability.SUBMIT_EVIDENCE),
                List.of(assignment),
                false,
                false
        );
        TaskBoardTask publicTask = task(TaskBoardAudience.PUBLIC);
        TaskBoardTask qaTask = task(TaskBoardAudience.QA_TESTER);

        assertTrue(foundingTester.canSubmitEvidence(publicTask, QaEnvironmentGrant.DEV));
        assertFalse(foundingTester.canSubmitEvidence(qaTask, QaEnvironmentGrant.DEV));
        assertFalse(foundingTester.canViewAudience(TaskBoardAudience.QA_TESTER, taskId));
        assertFalse(foundingTester.canViewAudience(TaskBoardAudience.STAFF, taskId));
    }

    @Test
    void humanVerifyRequiresLinkedIdentityCapabilityEnvironmentAndTaskAssignment() {
        QaSubjectContext unlinked = verifier(null, List.of(TaskBoardTestSubjects.assignment(
                taskId, QaCapability.HUMAN_VERIFY, QaEnvironmentGrant.DEV
        )));
        QaSubjectContext unassigned = verifier(minecraftUuid, List.of());
        QaSubjectContext assigned = verifier(minecraftUuid, List.of(TaskBoardTestSubjects.assignment(
                taskId, QaCapability.HUMAN_VERIFY, QaEnvironmentGrant.DEV
        )));

        assertFalse(unlinked.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
        assertFalse(unassigned.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
        assertFalse(assigned.canHumanVerify(taskId, QaEnvironmentGrant.PROD));
        assertTrue(assigned.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
    }

    @Test
    void technicalQaRequiresRegularTierThreeAndActiveAgreement() {
        QaTaskAssignmentGrant assignment = TaskBoardTestSubjects.assignment(
                taskId,
                QaCapability.TECHNICAL_DEV_EXECUTION,
                QaEnvironmentGrant.DEV
        );
        QaSubjectContext withoutAgreement = TaskBoardTestSubjects.active(
                accountId,
                minecraftUuid,
                Set.of(QaClass.REGULAR, QaClass.TECHNICAL),
                Optional.of(QaTier.TIER_III),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.TECHNICAL_DEV_EXECUTION),
                List.of(assignment),
                false,
                false
        );
        QaSubjectContext withAgreement = TaskBoardTestSubjects.active(
                accountId,
                minecraftUuid,
                Set.of(QaClass.REGULAR, QaClass.TECHNICAL),
                Optional.of(QaTier.TIER_III),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.TECHNICAL_DEV_EXECUTION),
                List.of(assignment),
                true,
                false
        );

        assertFalse(withoutAgreement.hasTechnicalDevExecution(taskId));
        assertTrue(withAgreement.hasTechnicalDevExecution(taskId));
        assertFalse(withAgreement.hasEnvironmentGrant(QaEnvironmentGrant.STAGING));
    }

    @Test
    void inactiveAccountOrMissingServiceBindingFailsClosedEvenForGlobalSuperuser() {
        QaSubjectContext disabled = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectStatus.DISABLED,
                Optional.of(minecraftUuid),
                Optional.of(UUID.randomUUID()),
                true,
                true,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.HUMAN_VERIFY, QaCapability.MANAGE_PROJECTS),
                List.of(),
                false
        );
        QaSubjectContext unboundGlobal = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectStatus.ACTIVE,
                Optional.of(minecraftUuid),
                Optional.empty(),
                false,
                true,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.HUMAN_VERIFY, QaCapability.MANAGE_PROJECTS),
                List.of(),
                false
        );

        assertFalse(disabled.canManageProject());
        assertFalse(disabled.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
        assertFalse(unboundGlobal.canManageProject());
        assertFalse(unboundGlobal.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
    }

    @Test
    void globalSuperuserStillNeedsLinkedMinecraftForHumanVerify() {
        QaSubjectContext noMinecraft = TaskBoardTestSubjects.active(
                accountId,
                null,
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                false,
                true
        );
        QaSubjectContext linked = TaskBoardTestSubjects.active(
                accountId,
                minecraftUuid,
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                false,
                true
        );

        assertTrue(noMinecraft.canManageProject());
        assertFalse(noMinecraft.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
        assertTrue(linked.canHumanVerify(taskId, QaEnvironmentGrant.DEV));
    }

    private QaSubjectContext verifier(UUID minecraft, List<QaTaskAssignmentGrant> assignments) {
        return TaskBoardTestSubjects.active(
                accountId,
                minecraft,
                Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_I),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.HUMAN_VERIFY),
                assignments,
                false,
                false
        );
    }

    private TaskBoardTask task(TaskBoardAudience audience) {
        Instant now = Instant.parse("2026-10-23T00:00:00Z");
        return new TaskBoardTask(
                taskId,
                "project-novus",
                "general",
                "Task",
                "Public task",
                "Description",
                "Summary",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.NORMAL,
                TaskBoardQaStatus.NHV,
                audience,
                1,
                false,
                "",
                "",
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                now,
                now
        );
    }
}
