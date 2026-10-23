package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardVqcContribution;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardActionCreateRequest;
import org.tavall.qa.request.TaskBoardAssignmentCreateRequest;
import org.tavall.qa.request.TaskBoardAssignmentRevokeRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardEvidenceReviewRequest;
import org.tavall.qa.request.TaskBoardEvidenceSubmissionRequest;
import org.tavall.qa.request.TaskBoardHumanVerificationRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskAcceptanceRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public final class TaskBoardMutationHandler implements ITaskBoardMutationHandler {
    private static final Pattern PROJECT_KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,95}$");
    private static final Pattern SECTION_KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,127}$");
    private static final Set<QaCapability> ASSIGNABLE_CAPABILITIES = Set.of(
            QaCapability.VIEW_QA_TASKS,
            QaCapability.SUBMIT_EVIDENCE,
            QaCapability.REVIEW_EVIDENCE,
            QaCapability.ADD_COMMENT,
            QaCapability.HUMAN_VERIFY
    );

    private final ITaskBoardDataHandler dataHandler;
    private final ITaskBoardActionTargetResolver actionTargetResolver;

    public TaskBoardMutationHandler(
            ITaskBoardDataHandler dataHandler,
            ITaskBoardActionTargetResolver actionTargetResolver
    ) {
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.actionTargetResolver = Objects.requireNonNull(actionTargetResolver, "actionTargetResolver");
    }

    @Override
    public TaskBoardProject createProject(QaSubjectContext subject, TaskBoardProjectCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canManageProject(), "Project management capability required");
        requireAvailable();

        String projectKey = normalizeProjectKey(request.projectKey());
        if (dataHandler.findProject(projectKey).isPresent()) {
            throw new IllegalArgumentException("Task Board project already exists: " + projectKey);
        }
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                projectKey,
                request.displayName(),
                request.description(),
                Objects.requireNonNull(request.status(), "status"),
                Objects.requireNonNull(request.audience(), "audience"),
                request.sortOrder(),
                now,
                now
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.empty(),
                projectKey,
                TaskBoardAuditEventType.PROJECT_CREATED,
                "Created project " + projectKey,
                now
        );
        return dataHandler.saveProject(project, auditEntry);
    }

    @Override
    public TaskBoardTask createTask(QaSubjectContext subject, TaskBoardTaskCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canManageTasks(), "Task management capability required");
        requireAvailable();

        String projectKey = normalizeProjectKey(request.projectKey());
        String sectionKey = normalizeSectionKey(request.sectionKey());
        TaskBoardProject project = dataHandler.findProject(projectKey)
                .orElseThrow(() -> new IllegalArgumentException("Task Board project not found: " + projectKey));
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardTask task = new TaskBoardTask(
                taskId,
                projectKey,
                sectionKey,
                request.title(),
                request.publicTitle(),
                request.description(),
                request.publicSummary(),
                TaskBoardTaskStatus.BACKLOG,
                Objects.requireNonNull(request.priority(), "priority"),
                TaskBoardQaStatus.NHV,
                Objects.requireNonNull(request.audience(), "audience"),
                request.weight(),
                request.hostValidationRequired(),
                request.sourceType(),
                request.sourceReference(),
                request.qaQcEligible(),
                subject.accountId().orElseThrow(),
                subject.linkedMinecraftUuid().orElse(null),
                null,
                null,
                null,
                null,
                request.sortOrder(),
                now,
                now
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(taskId),
                projectKey,
                TaskBoardAuditEventType.TASK_CREATED,
                "Created task in section " + sectionKey + " with audience " + task.audience(),
                now
        );
        return dataHandler.saveTask(task, Optional.empty(), auditEntry);
    }

    @Override
    public TaskBoardTask updateTask(QaSubjectContext subject, TaskBoardTaskUpdateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canManageTask(request.taskId()), "Task management capability required");
        requireAvailable();

        TaskBoardTask current = requireTask(request.taskId());
        if (request.qaStatus() != null && request.qaStatus() != current.qaStatus()) {
            throw new SecurityException("QA status must change through evidence review or Human Verify");
        }

        String sectionKey = request.sectionKey() == null
                ? current.sectionKey()
                : normalizeSectionKey(request.sectionKey());
        String title = request.title() == null ? current.title() : request.title();
        String publicTitle = request.publicTitle() == null ? current.publicTitle() : request.publicTitle();
        String description = request.description() == null ? current.description() : request.description();
        String publicSummary = request.publicSummary() == null ? current.publicSummary() : request.publicSummary();
        TaskBoardTaskStatus status = request.status() == null ? current.status() : request.status();
        TaskBoardTaskPriority priority = request.priority() == null ? current.priority() : request.priority();
        TaskBoardAudience audience = request.audience() == null ? current.audience() : request.audience();
        int weight = request.weight() == null ? current.weight() : request.weight();
        boolean hostValidationRequired = request.hostValidationRequired() == null
                ? current.hostValidationRequired()
                : request.hostValidationRequired();
        boolean qaQcEligible = request.qaQcEligible() == null ? current.qaQcEligible() : request.qaQcEligible();
        String sourceType = request.sourceType() == null ? current.sourceType() : request.sourceType();
        String sourceReference = request.sourceReference() == null
                ? current.sourceReference()
                : request.sourceReference();
        int sortOrder = request.sortOrder() == null ? current.sortOrder() : request.sortOrder();

        List<String> changedFields = changedFields(
                current,
                sectionKey,
                title,
                publicTitle,
                description,
                publicSummary,
                status,
                priority,
                audience,
                weight,
                hostValidationRequired,
                qaQcEligible,
                sourceType,
                sourceReference,
                sortOrder
        );
        if (changedFields.isEmpty()) {
            return current;
        }

        boolean materialChange = changedFields.stream().anyMatch(field -> !field.equals("status")
                && !field.equals("sortOrder"))
                || current.status() == TaskBoardTaskStatus.DONE && status != TaskBoardTaskStatus.DONE;
        TaskBoardQaStatus qaStatus = current.qaStatus();
        UUID verifiedByAccountId = current.verifiedByAccountId();
        UUID verifiedByMinecraftUuid = current.verifiedByMinecraftUUID();
        UUID verifiedEvidenceId = current.verifiedEvidenceId();
        Instant humanVerifiedAt = current.humanVerifiedAt();
        if (current.humanVerified() && (materialChange || status != TaskBoardTaskStatus.DONE)) {
            qaStatus = TaskBoardQaStatus.REVIEW_REQUIRED;
            verifiedByAccountId = null;
            verifiedByMinecraftUuid = null;
            verifiedEvidenceId = null;
            humanVerifiedAt = null;
        }

        Instant now = Instant.now();
        TaskBoardTask updated = new TaskBoardTask(
                current.taskId(),
                current.projectKey(),
                sectionKey,
                title,
                publicTitle,
                description,
                publicSummary,
                status,
                priority,
                qaStatus,
                audience,
                weight,
                hostValidationRequired,
                sourceType,
                sourceReference,
                qaQcEligible,
                current.qaRevision() + (materialChange ? 1 : 0),
                current.createdByAccountId(),
                current.createdByMinecraftUUID(),
                verifiedByAccountId,
                verifiedByMinecraftUuid,
                verifiedEvidenceId,
                humanVerifiedAt,
                sortOrder,
                current.createdAt(),
                now
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(updated.taskId()),
                updated.projectKey(),
                TaskBoardAuditEventType.TASK_UPDATED,
                "Updated task fields: " + String.join(", ", changedFields)
                        + (materialChange && current.humanVerified() ? "; prior Human Verify invalidated" : ""),
                now
        );
        return dataHandler.saveTask(updated, Optional.of(current.updatedAt()), auditEntry);
    }

    @Override
    public TaskBoardTask verifyTask(QaSubjectContext subject, TaskBoardHumanVerificationRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        requireAvailable();
        TaskBoardTask current = requireTask(request.taskId());
        if (current.status() != TaskBoardTaskStatus.DONE) {
            throw new IllegalArgumentException("Only DONE tasks can be Human Verified");
        }
        require(subject.canHumanVerify(current.taskId(), request.environment()),
                "Human Verify requires an active linked Minecraft identity, current QA authorization, and task assignment");
        TaskBoardEvidence evidence = dataHandler.findEvidenceById(request.evidenceId())
                .orElseThrow(() -> new IllegalArgumentException("QA evidence not found: " + request.evidenceId()));
        String currentFingerprint = TaskBoardTaskFingerprint.compute(current);
        if (!evidence.taskId().equals(current.taskId())
                || evidence.status() != TaskBoardEvidenceStatus.ACCEPTED
                || evidence.environment() != request.environment()
                || !evidence.currentFor(currentFingerprint)) {
            throw new SecurityException("Human Verify requires accepted evidence for the current task version and environment");
        }

        Instant now = Instant.now();
        UUID accountId = subject.accountId().orElseThrow();
        UUID minecraftUuid = subject.linkedMinecraftUuid().orElseThrow();
        TaskBoardTask verified = copyWithQaState(
                current,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                accountId,
                minecraftUuid,
                evidence.evidenceId(),
                now,
                now,
                current.qaRevision()
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(current.taskId()),
                current.projectKey(),
                TaskBoardAuditEventType.TASK_HUMAN_VERIFIED,
                "Human Verified using accepted evidence " + evidence.evidenceId(),
                now
        );
        return dataHandler.saveTask(verified, Optional.of(current.updatedAt()), auditEntry);
    }

    @Override
    public TaskBoardTask rejectTask(
            QaSubjectContext subject,
            UUID taskId,
            QaEnvironmentGrant environment,
            String reason
    ) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(environment, "environment");
        requireAccount(subject);
        requireAvailable();
        TaskBoardTask current = requireTask(taskId);
        require(subject.canAcceptReject(taskId, environment), "Evidence review capability and task assignment required");
        String safeReason = requireText(reason, "reason");
        Instant now = Instant.now();
        TaskBoardTask rejected = copyWithQaState(
                current,
                TaskBoardQaStatus.REVIEW_REQUIRED,
                null,
                null,
                null,
                null,
                now,
                current.qaRevision() + 1
        );
        rejected = copyWithStatus(rejected, TaskBoardTaskStatus.REVIEW, now);
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(taskId),
                current.projectKey(),
                TaskBoardAuditEventType.TASK_REJECTED,
                "Task rejected: " + safeReason,
                now
        );
        return dataHandler.saveTask(rejected, Optional.of(current.updatedAt()), auditEntry);
    }

    @Override
    public TaskBoardAssignment assignTask(QaSubjectContext subject, TaskBoardAssignmentCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canAssignTask(), "Task assignment capability required");
        requireAvailable();
        TaskBoardTask task = requireTask(request.taskId());
        require(ASSIGNABLE_CAPABILITIES.contains(request.capability()),
                "This capability cannot be granted through a task assignment");
        require(subject.hasEnvironmentGrant(request.environment()),
                "Assignment requires the actor to hold the same environment grant");
        if (request.requiredClass().orElse(null) == QaClass.TECHNICAL) {
            require(request.minimumTier().filter(tier -> tier.isAtLeast(QaTier.TIER_III)).isPresent(),
                    "Technical QA assignments require a Tier III minimum");
        }
        Instant now = Instant.now();
        TaskBoardAssignment assignment = new TaskBoardAssignment(
                UUID.randomUUID(),
                subject.organizationId().orElseThrow(),
                task.taskId(),
                request.accountId(),
                request.capability(),
                request.environment(),
                request.requiredClass(),
                request.minimumTier(),
                subject.accountId().orElseThrow(),
                request.reason(),
                now,
                null
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                TaskBoardAuditEventType.TASK_ASSIGNED,
                "Assigned " + request.capability() + " in " + request.environment() + " to Account "
                        + request.accountId(),
                now
        );
        return dataHandler.saveAssignment(assignment, auditEntry);
    }

    @Override
    public TaskBoardAssignment revokeAssignment(QaSubjectContext subject, TaskBoardAssignmentRevokeRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canAssignTask(), "Task assignment capability required");
        requireAvailable();
        TaskBoardAssignment current = dataHandler.findAssignment(request.assignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Task assignment not found: " + request.assignmentId()));
        TaskBoardTask task = requireTask(current.taskId());
        Instant now = Instant.now();
        TaskBoardAssignment revoked = current.revoke(now);
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                TaskBoardAuditEventType.TASK_ASSIGNMENT_REVOKED,
                "Revoked assignment " + current.assignmentId() + ": " + requireText(request.reason(), "reason"),
                now
        );
        return dataHandler.saveAssignment(revoked, auditEntry);
    }

    @Override
    public TaskBoardEvidence submitEvidence(QaSubjectContext subject, TaskBoardEvidenceSubmissionRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        requireAvailable();
        TaskBoardTask task = requireTask(request.taskId());
        require(subject.canSubmitEvidence(task, request.environment()),
                "Evidence submission requires an active QA class, environment grant, capability, and task assignment");
        String evidenceReference = normalizeEvidenceReference(request.evidenceReference());
        Instant now = Instant.now();
        TaskBoardEvidence evidence = new TaskBoardEvidence(
                UUID.randomUUID(),
                task.taskId(),
                subject.accountId().orElseThrow(),
                subject.linkedMinecraftUuid().orElse(null),
                subject.qaClasses(),
                subject.qaTier(),
                request.environment(),
                request.buildReference(),
                request.testCase(),
                request.expectedResult(),
                request.actualResult(),
                evidenceReference,
                request.publicSummary(),
                false,
                TaskBoardTaskFingerprint.compute(task),
                TaskBoardEvidenceStatus.PENDING_REVIEW,
                null,
                "",
                now,
                null
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                TaskBoardAuditEventType.EVIDENCE_SUBMITTED,
                "Submitted evidence " + evidence.evidenceId() + " for " + request.environment(),
                now
        );
        return dataHandler.saveEvidence(evidence, auditEntry);
    }

    @Override
    public TaskBoardEvidence reviewEvidence(QaSubjectContext subject, TaskBoardEvidenceReviewRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        requireAvailable();
        TaskBoardEvidence current = dataHandler.findEvidenceById(request.evidenceId())
                .orElseThrow(() -> new IllegalArgumentException("QA evidence not found: " + request.evidenceId()));
        TaskBoardTask task = requireTask(current.taskId());
        require(subject.canReviewEvidence(task.taskId(), request.environment()),
                "Evidence review capability, environment grant, and QA qualification required");
        require(current.environment() == request.environment(), "Review environment must match the submission");
        TaskBoardEvidenceStatus result = Objects.requireNonNull(request.result(), "result");
        if (result == TaskBoardEvidenceStatus.ACCEPTED
                && !current.currentFor(TaskBoardTaskFingerprint.compute(task))) {
            throw new IllegalStateException("Evidence is stale for the current task version and cannot be accepted");
        }
        Instant now = Instant.now();
        TaskBoardEvidence reviewed = current.review(
                result,
                subject.accountId().orElseThrow(),
                request.publicSummaryApproved(),
                request.reason(),
                now
        );
        TaskBoardAuditEventType eventType = result == TaskBoardEvidenceStatus.ACCEPTED
                ? TaskBoardAuditEventType.EVIDENCE_ACCEPTED
                : TaskBoardAuditEventType.EVIDENCE_REJECTED;
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                eventType,
                result + " evidence " + current.evidenceId()
                        + (request.reason().isBlank() ? "" : ": " + request.reason().trim()),
                now
        );
        return dataHandler.reviewEvidence(reviewed, auditEntry);
    }

    @Override
    public TaskBoardTask acceptTask(QaSubjectContext subject, TaskBoardTaskAcceptanceRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        requireAvailable();
        TaskBoardTask current = requireTask(request.taskId());
        require(subject.canAcceptReject(current.taskId(), request.environment()),
                "Task acceptance capability and environment-scoped review authorization required");
        if (current.status() != TaskBoardTaskStatus.REVIEW && current.status() != TaskBoardTaskStatus.DONE) {
            throw new IllegalArgumentException("Only REVIEW or DONE tasks can be accepted");
        }
        TaskBoardEvidence evidence = dataHandler.findEvidenceById(request.evidenceId())
                .orElseThrow(() -> new IllegalArgumentException("QA evidence not found: " + request.evidenceId()));
        if (!evidence.taskId().equals(current.taskId())
                || evidence.status() != TaskBoardEvidenceStatus.ACCEPTED
                || evidence.environment() != request.environment()
                || !evidence.currentFor(TaskBoardTaskFingerprint.compute(current))) {
            throw new SecurityException("Task acceptance requires accepted evidence for the current task version and environment");
        }
        if (current.humanVerified() && current.verifiedEvidenceId().equals(evidence.evidenceId())) {
            return current;
        }

        Instant now = Instant.now();
        TaskBoardTask accepted = copyWithQaState(
                current,
                TaskBoardQaStatus.NHV,
                null,
                null,
                null,
                null,
                now,
                current.qaRevision()
        );
        accepted = copyWithStatus(accepted, TaskBoardTaskStatus.DONE, now);
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(current.taskId()),
                current.projectKey(),
                TaskBoardAuditEventType.TASK_ACCEPTED,
                "Accepted task using evidence " + evidence.evidenceId() + "; task remains NHV until Human Verify",
                now
        );
        Optional<TaskBoardVqcContribution> contribution = current.qaQcEligible()
                ? Optional.of(new TaskBoardVqcContribution(
                        UUID.randomUUID(),
                        current.taskId(),
                        Optional.of(evidence.accountId()),
                        Optional.ofNullable(evidence.submitterMinecraftUuid()),
                        1,
                        0,
                        false,
                        "One accepted QA/QC task",
                        now
                ))
                : Optional.empty();
        return dataHandler.acceptTask(
                accepted,
                Optional.of(current.updatedAt()),
                evidence,
                auditEntry,
                contribution
        );
    }

    @Override
    public TaskBoardAction addAction(QaSubjectContext subject, TaskBoardActionCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        require(subject.canManageTask(request.taskId()), "Task management capability required");
        requireAvailable();
        TaskBoardTask task = requireTask(request.taskId());
        String target = actionTargetResolver.normalizeTarget(
                Objects.requireNonNull(request.type(), "type"),
                request.target()
        );
        Instant now = Instant.now();
        TaskBoardAction action = new TaskBoardAction(
                UUID.randomUUID(),
                request.taskId(),
                request.type(),
                request.label(),
                target,
                request.sortOrder(),
                now
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                TaskBoardAuditEventType.ACTION_ADDED,
                "Added task action " + request.type() + ": " + request.label().trim(),
                now
        );
        return dataHandler.saveAction(action, auditEntry);
    }

    @Override
    public TaskBoardComment addComment(QaSubjectContext subject, TaskBoardCommentCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAccount(subject);
        requireAvailable();
        TaskBoardTask task = requireTask(request.taskId());
        TaskBoardCommentType type = Objects.requireNonNull(request.type(), "type");
        if (type == TaskBoardCommentType.QA_NOTE) {
            require(subject.canReviewEvidence(task.taskId(), request.environment()),
                    "QA notes require evidence review authorization for this environment");
        } else {
            require(subject.canComment(task.taskId(), request.environment()),
                    "Task comment capability, environment grant, and assignment required");
        }
        validateParentComment(request.taskId(), request.parentCommentId());
        Instant now = Instant.now();
        TaskBoardComment comment = new TaskBoardComment(
                UUID.randomUUID(),
                request.taskId(),
                request.parentCommentId(),
                subject.accountId().orElseThrow(),
                subject.linkedMinecraftUuid().orElse(null),
                type,
                request.body(),
                now,
                now
        );
        TaskBoardAuditEntry auditEntry = audit(
                subject,
                Optional.of(task.taskId()),
                task.projectKey(),
                TaskBoardAuditEventType.COMMENT_ADDED,
                "Added " + type + " thread entry",
                now
        );
        return dataHandler.saveComment(comment, auditEntry);
    }

    private List<String> changedFields(
            TaskBoardTask current,
            String sectionKey,
            String title,
            String publicTitle,
            String description,
            String publicSummary,
            TaskBoardTaskStatus status,
            org.tavall.qa.domain.TaskBoardTaskPriority priority,
            org.tavall.qa.domain.TaskBoardAudience audience,
            int weight,
            boolean hostValidationRequired,
            boolean qaQcEligible,
            String sourceType,
            String sourceReference,
            int sortOrder
    ) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        if (!current.sectionKey().equals(sectionKey)) fields.add("sectionKey");
        if (!current.title().equals(title)) fields.add("title");
        if (!current.publicTitle().equals(publicTitle)) fields.add("publicTitle");
        if (!current.description().equals(description)) fields.add("description");
        if (!current.publicSummary().equals(publicSummary)) fields.add("publicSummary");
        if (current.status() != status) fields.add("status");
        if (current.priority() != priority) fields.add("priority");
        if (current.audience() != audience) fields.add("audience");
        if (current.weight() != weight) fields.add("weight");
        if (current.hostValidationRequired() != hostValidationRequired) fields.add("hostValidationRequired");
        if (current.qaQcEligible() != qaQcEligible) fields.add("qaQcEligible");
        if (!current.sourceType().equals(sourceType)) fields.add("sourceType");
        if (!current.sourceReference().equals(sourceReference)) fields.add("sourceReference");
        if (current.sortOrder() != sortOrder) fields.add("sortOrder");
        return List.copyOf(fields);
    }

    private TaskBoardTask copyWithQaState(
            TaskBoardTask task,
            TaskBoardQaStatus qaStatus,
            UUID verifiedByAccountId,
            UUID verifiedByMinecraftUuid,
            UUID evidenceId,
            Instant verifiedAt,
            Instant updatedAt,
            int qaRevision
    ) {
        return new TaskBoardTask(
                task.taskId(),
                task.projectKey(),
                task.sectionKey(),
                task.title(),
                task.publicTitle(),
                task.description(),
                task.publicSummary(),
                task.status(),
                task.priority(),
                qaStatus,
                task.audience(),
                task.weight(),
                task.hostValidationRequired(),
                task.sourceType(),
                task.sourceReference(),
                task.qaQcEligible(),
                qaRevision,
                task.createdByAccountId(),
                task.createdByMinecraftUUID(),
                verifiedByAccountId,
                verifiedByMinecraftUuid,
                evidenceId,
                verifiedAt,
                task.sortOrder(),
                task.createdAt(),
                updatedAt
        );
    }

    private TaskBoardTask copyWithStatus(TaskBoardTask task, TaskBoardTaskStatus status, Instant updatedAt) {
        return new TaskBoardTask(
                task.taskId(),
                task.projectKey(),
                task.sectionKey(),
                task.title(),
                task.publicTitle(),
                task.description(),
                task.publicSummary(),
                status,
                task.priority(),
                task.qaStatus(),
                task.audience(),
                task.weight(),
                task.hostValidationRequired(),
                task.sourceType(),
                task.sourceReference(),
                task.qaQcEligible(),
                task.qaRevision(),
                task.createdByAccountId(),
                task.createdByMinecraftUUID(),
                task.verifiedByAccountId(),
                task.verifiedByMinecraftUUID(),
                task.verifiedEvidenceId(),
                task.humanVerifiedAt(),
                task.sortOrder(),
                task.createdAt(),
                updatedAt
        );
    }

    private TaskBoardTask requireTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return dataHandler.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId));
    }

    private void validateParentComment(UUID taskId, UUID parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        boolean belongsToTask = dataHandler.findComments(taskId).stream()
                .anyMatch(comment -> comment.commentId().equals(parentCommentId));
        if (!belongsToTask) {
            throw new IllegalArgumentException("Parent comment does not belong to this Task Board task");
        }
    }

    private TaskBoardAuditEntry audit(
            QaSubjectContext subject,
            Optional<UUID> taskId,
            String projectKey,
            TaskBoardAuditEventType eventType,
            String summary,
            Instant at
    ) {
        return new TaskBoardAuditEntry(
                UUID.randomUUID(),
                taskId,
                projectKey,
                Optional.of(subject.accountId().orElseThrow()),
                subject.linkedMinecraftUuid().orElse(null),
                eventType,
                summary,
                at
        );
    }

    private String normalizeEvidenceReference(String rawReference) {
        String value = requireText(rawReference, "evidenceReference");
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Evidence references must be HTTPS URLs without embedded credentials");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Evidence reference must be a valid HTTPS URL", exception);
        }
    }

    private void requireAccount(QaSubjectContext subject) {
        Objects.requireNonNull(subject, "subject");
        if (!subject.isAccountActive() || !subject.hasQaServiceContext()) {
            throw new SecurityException("An active Tavall Account with the Web service binding is required");
        }
    }

    private void requireAvailable() {
        if (!dataHandler.isAvailable()) {
            throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new SecurityException(message);
        }
    }

    private String normalizeProjectKey(String projectKey) {
        return normalizeKey(projectKey, PROJECT_KEY, "Project");
    }

    private String normalizeSectionKey(String sectionKey) {
        return normalizeKey(sectionKey, SECTION_KEY, "Section");
    }

    private String normalizeKey(String value, Pattern pattern, String label) {
        Objects.requireNonNull(value, label.toLowerCase(Locale.ROOT) + "Key");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(label + " keys use lowercase letters, numbers, and hyphens");
        }
        return normalized;
    }

    private String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
