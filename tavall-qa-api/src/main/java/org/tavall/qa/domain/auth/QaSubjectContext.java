package org.tavall.qa.domain.auth;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Request-time Account and task-assignment facts used by QA query/mutation boundaries. */
public record QaSubjectContext(
        Optional<UUID> accountId,
        QaSubjectStatus status,
        Optional<UUID> linkedMinecraftUuid,
        Optional<UUID> organizationId,
        boolean hasActiveServiceBinding,
        boolean isAccountGlobalSuperuser,
        Set<QaClass> qaClasses,
        Optional<QaTier> qaTier,
        Set<QaEnvironmentGrant> environmentGrants,
        Set<QaCapability> grantedCapabilities,
        List<QaTaskAssignmentGrant> taskAssignments,
        boolean technicalAgreementActive
) {
    public QaSubjectContext {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(linkedMinecraftUuid, "linkedMinecraftUuid");
        Objects.requireNonNull(organizationId, "organizationId");
        qaClasses = Set.copyOf(Objects.requireNonNull(qaClasses, "qaClasses"));
        Objects.requireNonNull(qaTier, "qaTier");
        environmentGrants = Set.copyOf(Objects.requireNonNull(environmentGrants, "environmentGrants"));
        grantedCapabilities = Set.copyOf(Objects.requireNonNull(grantedCapabilities, "grantedCapabilities"));
        taskAssignments = List.copyOf(Objects.requireNonNull(taskAssignments, "taskAssignments"));
    }

    public static QaSubjectContext anonymous() {
        return new QaSubjectContext(
                Optional.empty(),
                QaSubjectStatus.ANONYMOUS,
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
    }

    public static QaSubjectContext unavailable(Optional<UUID> accountId) {
        return new QaSubjectContext(
                Objects.requireNonNull(accountId, "accountId"),
                QaSubjectStatus.UNAVAILABLE,
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
    }

    public boolean isAnonymous() {
        return accountId.isEmpty();
    }

    public boolean isAccountActive() {
        return accountId.isPresent() && status == QaSubjectStatus.ACTIVE;
    }

    public boolean hasQaServiceContext() {
        return isAccountActive() && organizationId.isPresent() && hasActiveServiceBinding;
    }

    public boolean isGlobalQaAdministrator() {
        return hasQaServiceContext() && isAccountGlobalSuperuser;
    }

    public boolean hasCapability(QaCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return hasQaServiceContext() && grantedCapabilities.contains(capability);
    }

    public boolean hasEnvironmentGrant(QaEnvironmentGrant grant) {
        Objects.requireNonNull(grant, "grant");
        return hasQaServiceContext()
                && (isAccountGlobalSuperuser || environmentGrants.contains(grant));
    }

    public boolean canViewAudience(TaskBoardAudience audience, UUID taskId) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(taskId, "taskId");
        if (audience == TaskBoardAudience.PUBLIC) {
            return true;
        }
        if (!hasQaServiceContext()) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        return switch (audience) {
            case PUBLIC -> true;
            case QA_TESTER -> (hasClass(QaClass.REGULAR) || hasClass(QaClass.TECHNICAL))
                    && qaTier.isPresent()
                    && (hasCapability(QaCapability.VIEW_QA_TASKS)
                    || isAssignedToTask(taskId));
            case STAFF -> hasCapability(QaCapability.VIEW_STAFF_TASKS)
                    || hasCapability(QaCapability.MANAGE_TASKS);
            case ADMIN_ONLY -> hasCapability(QaCapability.MANAGE_PROJECTS);
        };
    }

    public boolean canViewProjectAudience(TaskBoardAudience audience) {
        Objects.requireNonNull(audience, "audience");
        if (audience == TaskBoardAudience.PUBLIC) {
            return true;
        }
        if (!hasQaServiceContext()) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        return switch (audience) {
            case PUBLIC -> true;
            case QA_TESTER -> (hasClass(QaClass.REGULAR) || hasClass(QaClass.TECHNICAL))
                    && qaTier.isPresent()
                    && hasCapability(QaCapability.VIEW_QA_TASKS);
            case STAFF -> hasCapability(QaCapability.VIEW_STAFF_TASKS)
                    || hasCapability(QaCapability.MANAGE_TASKS);
            case ADMIN_ONLY -> hasCapability(QaCapability.MANAGE_PROJECTS);
        };
    }

    public boolean canViewStaffTask(TaskBoardTask task) {
        Objects.requireNonNull(task, "task");
        return hasQaServiceContext()
                && (isAccountGlobalSuperuser
                || hasCapability(QaCapability.VIEW_STAFF_TASKS)
                || canManageTask(task.taskId())
                || hasCapability(QaCapability.VIEW_STAFF_WORKSPACE));
    }

    public boolean isAssignedToTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return taskAssignments.stream().anyMatch(assignment ->
                assignment.taskId().equals(taskId) && assignmentMatchesQualification(assignment));
    }

    public boolean canViewTask(TaskBoardProject project, TaskBoardTask task) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(task, "task");
        if (!project.projectKey().equals(task.projectKey())) {
            return false;
        }
        if (project.publicVisible() && task.publicVisible()) {
            return true;
        }
        return canViewAudience(project.audience(), task.taskId())
                && canViewAudience(task.audience(), task.taskId());
    }

    public boolean canViewStaffWorkspace() {
        return hasCapability(QaCapability.VIEW_STAFF_WORKSPACE)
                || hasCapability(QaCapability.MANAGE_TASKS)
                || isGlobalQaAdministrator();
    }

    public boolean canViewWorkspaceForTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return canViewStaffWorkspace();
    }

    public boolean canManageProject() {
        return hasCapability(QaCapability.MANAGE_PROJECTS) || isGlobalQaAdministrator();
    }

    public boolean canManageTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return canManageTasks();
    }

    public boolean canManageTasks() {
        return hasCapability(QaCapability.MANAGE_TASKS) || isGlobalQaAdministrator();
    }

    public boolean canAssignTask() {
        return hasCapability(QaCapability.ASSIGN_OTHERS) || isGlobalQaAdministrator();
    }

    public boolean canEditQaMessages() {
        return hasCapability(QaCapability.MANAGE_QA_MESSAGES) || isGlobalQaAdministrator();
    }

    public boolean canViewQaProgression(UUID targetAccountId) {
        Objects.requireNonNull(targetAccountId, "targetAccountId");
        if (!hasQaServiceContext()) {
            return false;
        }
        return isAccountGlobalSuperuser
                || hasCapability(QaCapability.VIEW_QA_PROGRESS)
                || accountId.filter(targetAccountId::equals).isPresent()
                && (!qaClasses.isEmpty() || qaTier.isPresent());
    }

    public boolean canComment(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return hasQaServiceContext()
                && (hasCapability(QaCapability.ADD_COMMENT)
                && hasTaskAssignment(taskId, QaCapability.ADD_COMMENT)
                || canManageTask(taskId));
    }

    public boolean canComment(UUID taskId, QaEnvironmentGrant environment) {
        Objects.requireNonNull(environment, "environment");
        return hasEnvironmentGrant(environment)
                && (hasCapability(QaCapability.ADD_COMMENT)
                && hasTaskAssignment(taskId, QaCapability.ADD_COMMENT, environment)
                || canManageTask(taskId));
    }

    public boolean canSubmitEvidence(TaskBoardTask task, QaEnvironmentGrant environment) {
        Objects.requireNonNull(task, "task");
        return canSubmitEvidence(task.taskId(), task.audience(), task.status(), environment);
    }

    public boolean canSubmitEvidence(
            UUID taskId,
            TaskBoardAudience audience,
            TaskBoardTaskStatus status,
            QaEnvironmentGrant environment
    ) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(environment, "environment");
        if (!hasCapability(QaCapability.SUBMIT_EVIDENCE)
                || !hasEnvironmentGrant(environment)
                || !hasTaskAssignment(taskId, QaCapability.SUBMIT_EVIDENCE, environment)
                || status == TaskBoardTaskStatus.CANCELLED) {
            return false;
        }
        if (hasClass(QaClass.FOUNDING_PUBLIC)) {
            return audience == TaskBoardAudience.PUBLIC
                    && hasCapability(QaCapability.PARTICIPATE_PUBLIC_QA);
        }
        return hasRegularQaQualification()
                && (!hasClass(QaClass.TECHNICAL) || hasTechnicalQaQualification());
    }

    public boolean canReviewEvidence(UUID taskId, QaEnvironmentGrant environment) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(environment, "environment");
        if (!hasQaServiceContext() || !hasEnvironmentGrant(environment)) {
            return false;
        }
        if (isAccountGlobalSuperuser) {
            return true;
        }
        if (!hasCapability(QaCapability.REVIEW_EVIDENCE) || !hasReviewQualification()) {
            return false;
        }
        return hasTaskAssignment(taskId, QaCapability.REVIEW_EVIDENCE, environment)
                || canManageTask(taskId);
    }

    public boolean canAcceptReject(UUID taskId, QaEnvironmentGrant environment) {
        return isGlobalQaAdministrator()
                || hasCapability(QaCapability.ACCEPT_REJECT_TASK)
                && canReviewEvidence(taskId, environment);
    }

    public boolean canHumanVerify(UUID taskId, QaEnvironmentGrant environment) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(environment, "environment");
        if (!hasQaServiceContext()
                || linkedMinecraftUuid.isEmpty()
                || !hasEnvironmentGrant(environment)
                || !isAccountGlobalSuperuser
                && (!hasCapability(QaCapability.HUMAN_VERIFY)
                || !hasTaskAssignment(taskId, QaCapability.HUMAN_VERIFY, environment))) {
            return false;
        }
        return isAccountGlobalSuperuser || hasHumanVerifyQualification()
                && (!hasClass(QaClass.TECHNICAL) || hasTechnicalQaQualification());
    }

    public boolean hasTechnicalDevExecution(UUID taskId) {
        return hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION)
                && hasEnvironmentGrant(QaEnvironmentGrant.DEV)
                && hasTechnicalQaQualification()
                && hasTaskAssignment(taskId, QaCapability.TECHNICAL_DEV_EXECUTION, QaEnvironmentGrant.DEV);
    }

    private boolean hasClass(QaClass qaClass) {
        return qaClasses.contains(qaClass);
    }

    private boolean hasRegularQaQualification() {
        return hasClass(QaClass.REGULAR) && qaTier.isPresent();
    }

    private boolean hasTechnicalQaQualification() {
        return hasClass(QaClass.REGULAR)
                && hasClass(QaClass.TECHNICAL)
                && qaTier.filter(tier -> tier.isAtLeast(QaTier.TIER_III)).isPresent()
                && technicalAgreementActive;
    }

    private boolean hasHumanVerifyQualification() {
        return hasRegularQaQualification()
                && qaTier.filter(tier -> tier.isAtLeast(QaTier.TIER_I)).isPresent();
    }

    private boolean hasReviewQualification() {
        return hasRegularQaQualification()
                && qaTier.filter(tier -> tier.isAtLeast(QaTier.TIER_II)).isPresent()
                || hasTechnicalQaQualification();
    }

    private boolean hasTaskAssignment(UUID taskId, QaCapability capability) {
        return taskAssignments.stream().anyMatch(assignment ->
                assignmentMatchesOrganization(assignment)
                        && assignment.taskId().equals(taskId)
                        && assignment.capability() == capability
                        && assignmentMatchesQualification(assignment));
    }

    private boolean hasTaskAssignment(
            UUID taskId,
            QaCapability capability,
            QaEnvironmentGrant environment
    ) {
        return taskAssignments.stream().anyMatch(assignment ->
                assignmentMatchesOrganization(assignment)
                        && assignment.taskId().equals(taskId)
                        && assignment.capability() == capability
                        && assignment.environment() == environment
                        && assignmentMatchesQualification(assignment));
    }

    private boolean assignmentMatchesQualification(QaTaskAssignmentGrant assignment) {
        boolean classMatches = assignment.requiredClass().isEmpty()
                || qaClasses.contains(assignment.requiredClass().get());
        boolean tierMatches = assignment.minimumTier().isEmpty()
                || qaTier.filter(tier -> tier.isAtLeast(assignment.minimumTier().get())).isPresent();
        boolean technicalAgreementMatches = assignment.requiredClass().orElse(null) != QaClass.TECHNICAL
                || hasTechnicalQaQualification();
        return isAccountActive() && classMatches && tierMatches && technicalAgreementMatches;
    }

    private boolean assignmentMatchesOrganization(QaTaskAssignmentGrant assignment) {
        return organizationId.isPresent() && organizationId.get().equals(assignment.organizationId());
    }
}
