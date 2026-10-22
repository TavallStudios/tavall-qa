package org.tavall.qa.auth;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaTier;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardAuthTest {
    private final UUID accountId = UUID.randomUUID();
    private final UUID minecraftUuid = UUID.randomUUID();

    @Test
    void anonymousVisitorCanOnlyViewPublic() {
        QaSubjectContext anonymous = QaSubjectContext.anonymous();

        assertTrue(anonymous.isAnonymous());
        assertTrue(anonymous.canViewAudience(TaskBoardAudience.PUBLIC));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.QA_TESTER));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.STAFF));
        assertFalse(anonymous.canViewAudience(TaskBoardAudience.ADMIN_ONLY));
        assertFalse(anonymous.canSubmitEvidence(null));
        assertFalse(anonymous.canHumanVerify(null));
    }

    @Test
    void authenticatedUserWithoutQaPermissionsHasNoStaffAuthority() {
        QaSubjectContext plainUser = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectContext.STATUS_ACTIVE,
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Set.of()
        );

        assertFalse(plainUser.isAnonymous());
        assertTrue(plainUser.canViewAudience(TaskBoardAudience.PUBLIC));
        assertFalse(plainUser.canViewAudience(TaskBoardAudience.QA_TESTER));
        assertFalse(plainUser.canViewAudience(TaskBoardAudience.STAFF));
        assertFalse(plainUser.canViewAudience(TaskBoardAudience.ADMIN_ONLY));
        assertFalse(plainUser.canSubmitEvidence(null));
        assertFalse(plainUser.canHumanVerify(null));
    }

    @Test
    void foundingTesterCanViewQaTesterAudienceAndSubmitEvidence() {
        QaSubjectContext foundingTester = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectContext.STATUS_ACTIVE,
                Optional.empty(),
                false,
                Optional.of(QaClass.FOUNDING_PUBLIC),
                Optional.of(QaTier.TIER_0),
                Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.SUBMIT_EVIDENCE)
        );

        assertTrue(foundingTester.canViewAudience(TaskBoardAudience.PUBLIC));
        assertTrue(foundingTester.canViewAudience(TaskBoardAudience.QA_TESTER));
        assertFalse(foundingTester.canViewAudience(TaskBoardAudience.STAFF));
        assertTrue(foundingTester.canSubmitEvidence(null));
    }

    @Test
    void humanVerifyRequiresLinkedMinecraftIdentityAndCapability() {
        QaSubjectContext unlinkedLead = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectContext.STATUS_ACTIVE,
                Optional.empty(), // No Minecraft UUID linked
                false,
                Optional.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD),
                Set.of(QaCapability.HUMAN_VERIFY)
        );

        // Even Tier V QA cannot human verify without linked Minecraft UUID
        assertFalse(unlinkedLead.canHumanVerify(null));

        QaSubjectContext linkedLead = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectContext.STATUS_ACTIVE,
                Optional.of(minecraftUuid),
                false,
                Optional.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD),
                Set.of(QaCapability.HUMAN_VERIFY)
        );

        assertTrue(linkedLead.canHumanVerify(null));
    }

    @Test
    void disabledOrMergedAccountFailsClosed() {
        QaSubjectContext disabledAdmin = new QaSubjectContext(
                Optional.of(accountId),
                QaSubjectContext.STATUS_DISABLED,
                Optional.of(minecraftUuid),
                true, // Even with superuser flag
                Optional.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_V),
                Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD),
                Set.of(QaCapability.MANAGE_PROJECTS, QaCapability.HUMAN_VERIFY)
        );

        assertFalse(disabledAdmin.isAccountActive());
        assertTrue(disabledAdmin.canViewAudience(TaskBoardAudience.PUBLIC));
        assertFalse(disabledAdmin.canViewAudience(TaskBoardAudience.QA_TESTER));
        assertFalse(disabledAdmin.canViewAudience(TaskBoardAudience.STAFF));
        assertFalse(disabledAdmin.canViewAudience(TaskBoardAudience.ADMIN_ONLY));
        assertFalse(disabledAdmin.canHumanVerify(null));
        assertFalse(disabledAdmin.canSubmitEvidence(null));
    }
}
