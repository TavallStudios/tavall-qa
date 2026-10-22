package org.tavall.qa.domain.auth;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardTask;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record QaSubjectContext(
        Optional<UUID> accountId,
        String accountStatus,
        Optional<UUID> linkedMinecraftUuid,
        boolean isAccountGlobalSuperuser,
        Optional<QaClass> qaClass,
        Optional<QaTier> qaTier,
        Set<QaEnvironmentGrant> environmentGrants,
        Set<QaCapability> grantedCapabilities
) {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_MERGED = "MERGED";
    public static final String STATUS_DISABLED = "DISABLED";

    public QaSubjectContext {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(accountStatus, "accountStatus");
        Objects.requireNonNull(linkedMinecraftUuid, "linkedMinecraftUuid");
        Objects.requireNonNull(qaClass, "qaClass");
        Objects.requireNonNull(qaTier, "qaTier");
        environmentGrants = environmentGrants == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(environmentGrants);
        grantedCapabilities = grantedCapabilities == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(grantedCapabilities);
    }

    public static QaSubjectContext anonymous() {
        return new QaSubjectContext(
                Optional.empty(),
                STATUS_ACTIVE,
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Set.of()
        );
    }

    public boolean isAnonymous() {
        return accountId.isEmpty();
    }

    public boolean isAccountActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(accountStatus);
    }

    public boolean canViewAudience(TaskBoardAudience audience) {
        if (!isAccountActive()) {
            return audience == TaskBoardAudience.PUBLIC;
        }
        if (isAccountGlobalSuperuser || hasCapability(QaCapability.MANAGE_PROJECTS)) {
            return true;
        }
        return switch (audience) {
            case PUBLIC -> true;
            case QA_TESTER -> qaClass.isPresent() || isStaffMember();
            case STAFF -> isStaffMember();
            case ADMIN_ONLY -> isAdministrator();
        };
    }

    public boolean canSubmitEvidence(TaskBoardTask task) {
        if (!isAccountActive() || isAnonymous()) {
            return false;
        }
        if (isAccountGlobalSuperuser || hasCapability(QaCapability.SUBMIT_EVIDENCE)) {
            return true;
        }
        return qaClass.isPresent();
    }

    public boolean canHumanVerify(TaskBoardTask task) {
        if (!isAccountActive() || linkedMinecraftUuid.isEmpty()) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        return hasCapability(QaCapability.HUMAN_VERIFY);
    }

    public boolean canAcceptReject(TaskBoardTask task) {
        if (!isAccountActive()) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        return hasCapability(QaCapability.ACCEPT_REJECT_TASK);
    }

    public boolean canManageTask(TaskBoardTask task) {
        if (!isAccountActive()) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        return hasCapability(QaCapability.MANAGE_TASKS);
    }

    public boolean hasCapability(QaCapability capability) {
        return isAccountActive() && grantedCapabilities.contains(capability);
    }

    public boolean hasEnvironmentGrant(QaEnvironmentGrant grant) {
        return isAccountActive() && (isAccountGlobalSuperuser || environmentGrants.contains(grant));
    }

    private boolean isStaffMember() {
        return hasCapability(QaCapability.MANAGE_TASKS)
                || hasCapability(QaCapability.ACCEPT_REJECT_TASK)
                || (qaTier.isPresent() && qaTier.get().isAtLeast(QaTier.TIER_III));
    }

    private boolean isAdministrator() {
        return isAccountGlobalSuperuser || hasCapability(QaCapability.MANAGE_PROJECTS);
    }
}
