package org.tavall.qa.persistence;

import jakarta.persistence.LockModeType;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.entity.IPostgresEntityOperationContext;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardVqcContribution;
import org.tavall.qa.persistence.entity.TaskBoardActionEntity;
import org.tavall.qa.persistence.entity.TaskBoardAssignmentEntity;
import org.tavall.qa.persistence.entity.TaskBoardAuditEntryEntity;
import org.tavall.qa.persistence.entity.TaskBoardCommentEntity;
import org.tavall.qa.persistence.entity.TaskBoardEvidenceEntity;
import org.tavall.qa.persistence.entity.TaskBoardProjectEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;
import org.tavall.qa.persistence.entity.TaskBoardVqcContributionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TaskBoardDataHandler implements ITaskBoardDataHandler {
    private static final String TASK_LOCK_PREFIX = "tavall-qa:task:";
    private static final String EVIDENCE_LOCK_PREFIX = "tavall-qa:evidence:";
    private static final String ASSIGNMENT_LOCK_PREFIX = "tavall-qa:assignment:";
    private static final String VQC_LOCK_PREFIX = "tavall-qa:vqc:";

    private final IPostgresDatabase database;

    public TaskBoardDataHandler(IPostgresDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public boolean isAvailable() {
        return database.isAvailable() && database.entities().isOpen();
    }

    @Override
    public List<TaskBoardProject> findProjects() {
        requireAvailable();
        return database.entities().findNamed(
                        TaskBoardProjectEntity.class,
                        TaskBoardProjectEntity.FIND_ALL,
                        Map.of(),
                        0
                ).stream()
                .map(TaskBoardProjectEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardProject> findProject(String projectKey) {
        requireAvailable();
        return database.entities()
                .find(TaskBoardProjectEntity.class, requireText(projectKey, "projectKey"))
                .map(TaskBoardProjectEntity::toDomain);
    }

    @Override
    public TaskBoardProject saveProject(TaskBoardProject project, TaskBoardAuditEntry auditEntry) {
        requireAvailable();
        Objects.requireNonNull(project, "project");
        requireAudit(auditEntry, Optional.empty(), project.projectKey());
        return database.entities().executeAtomic(context -> {
            TaskBoardProjectEntity saved = context.save(TaskBoardProjectEntity.fromDomain(project));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardTask> findTasks(String projectKey) {
        requireAvailable();
        return database.entities().findNamed(
                        TaskBoardTaskEntity.class,
                        TaskBoardTaskEntity.FIND_BY_PROJECT,
                        Map.of("projectKey", requireText(projectKey, "projectKey")),
                        0
                ).stream()
                .map(TaskBoardTaskEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardTask> findTask(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities()
                .find(TaskBoardTaskEntity.class, taskId.toString())
                .map(TaskBoardTaskEntity::toDomain);
    }

    @Override
    public TaskBoardTask saveTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardAuditEntry auditEntry
    ) {
        requireAvailable();
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(expectedUpdatedAt, "expectedUpdatedAt");
        requireAudit(auditEntry, Optional.of(task.taskId()), task.projectKey());
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(TASK_LOCK_PREFIX + task.taskId());
            Optional<TaskBoardTaskEntity> persisted = context.find(
                    TaskBoardTaskEntity.class,
                    task.taskId().toString(),
                    LockModeType.PESSIMISTIC_WRITE
            );
            if (expectedUpdatedAt.isEmpty() && persisted.isPresent()) {
                throw new TaskBoardConcurrentModificationException("Task was created concurrently: " + task.taskId());
            }
            if (expectedUpdatedAt.isPresent()) {
                TaskBoardTaskEntity current = persisted.orElseThrow(
                        () -> new TaskBoardConcurrentModificationException("Task disappeared during update: " + task.taskId())
                );
                if (!current.toDomain().updatedAt().equals(expectedUpdatedAt.get())) {
                    throw new TaskBoardConcurrentModificationException("Task changed concurrently; reload before retrying");
                }
            }
            TaskBoardTaskEntity saved = context.save(TaskBoardTaskEntity.fromDomain(task));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardAction> findActions(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardActionEntity.class,
                        TaskBoardActionEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardActionEntity::toDomain)
                .toList();
    }

    @Override
    public TaskBoardAction saveAction(TaskBoardAction action, TaskBoardAuditEntry auditEntry) {
        requireAvailable();
        Objects.requireNonNull(action, "action");
        requireAudit(auditEntry, Optional.of(action.taskId()), requireTaskProjectKey(action.taskId()));
        return database.entities().executeAtomic(context -> {
            TaskBoardActionEntity saved = context.save(TaskBoardActionEntity.fromDomain(action));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardComment> findComments(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardCommentEntity.class,
                        TaskBoardCommentEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardCommentEntity::toDomain)
                .toList();
    }

    @Override
    public TaskBoardComment saveComment(TaskBoardComment comment, TaskBoardAuditEntry auditEntry) {
        requireAvailable();
        Objects.requireNonNull(comment, "comment");
        requireAudit(auditEntry, Optional.of(comment.taskId()), requireTaskProjectKey(comment.taskId()));
        return database.entities().executeAtomic(context -> {
            TaskBoardCommentEntity saved = context.save(TaskBoardCommentEntity.fromDomain(comment));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardAssignment> findAssignments(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardAssignmentEntity.class,
                        TaskBoardAssignmentEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardAssignmentEntity::toDomain)
                .toList();
    }

    @Override
    public List<TaskBoardAssignment> findAssignmentsForAccount(UUID accountId) {
        requireAvailable();
        Objects.requireNonNull(accountId, "accountId");
        return database.entities().findNamed(
                        TaskBoardAssignmentEntity.class,
                        TaskBoardAssignmentEntity.FIND_BY_ACCOUNT,
                        Map.of("accountId", accountId.toString()),
                        0
                ).stream()
                .map(TaskBoardAssignmentEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardAssignment> findAssignment(UUID assignmentId) {
        requireAvailable();
        Objects.requireNonNull(assignmentId, "assignmentId");
        return database.entities()
                .find(TaskBoardAssignmentEntity.class, assignmentId.toString())
                .map(TaskBoardAssignmentEntity::toDomain);
    }

    @Override
    public TaskBoardAssignment saveAssignment(TaskBoardAssignment assignment, TaskBoardAuditEntry auditEntry) {
        requireAvailable();
        Objects.requireNonNull(assignment, "assignment");
        requireAudit(auditEntry, Optional.of(assignment.taskId()), requireTaskProjectKey(assignment.taskId()));
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(ASSIGNMENT_LOCK_PREFIX + assignment.assignmentId());
            Optional<TaskBoardAssignmentEntity> persisted = context.find(
                    TaskBoardAssignmentEntity.class,
                    assignment.assignmentId().toString(),
                    LockModeType.PESSIMISTIC_WRITE
            );
            if (assignment.revokedAt() == null && persisted.isPresent()) {
                throw new IllegalStateException("Task assignment already exists: " + assignment.assignmentId());
            }
            if (assignment.revokedAt() != null) {
                TaskBoardAssignment current = persisted.map(TaskBoardAssignmentEntity::toDomain)
                        .orElseThrow(() -> new IllegalStateException("Task assignment disappeared before revocation"));
                if (!current.activeAt(assignment.revokedAt())) {
                    throw new IllegalStateException("Task assignment was already revoked");
                }
            }
            TaskBoardAssignmentEntity saved = context.save(TaskBoardAssignmentEntity.fromDomain(assignment));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardEvidence> findEvidenceForTask(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardEvidenceEntity.class,
                        TaskBoardEvidenceEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardEvidenceEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardEvidence> findEvidenceById(UUID evidenceId) {
        requireAvailable();
        Objects.requireNonNull(evidenceId, "evidenceId");
        return database.entities()
                .find(TaskBoardEvidenceEntity.class, evidenceId.toString())
                .map(TaskBoardEvidenceEntity::toDomain);
    }

    @Override
    public TaskBoardEvidence saveEvidence(TaskBoardEvidence evidence, TaskBoardAuditEntry auditEntry) {
        requireAvailable();
        Objects.requireNonNull(evidence, "evidence");
        requireAudit(auditEntry, Optional.of(evidence.taskId()), requireTaskProjectKey(evidence.taskId()));
        if (evidence.status() != TaskBoardEvidenceStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("New evidence must start pending review");
        }
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(EVIDENCE_LOCK_PREFIX + evidence.evidenceId());
            if (context.find(TaskBoardEvidenceEntity.class, evidence.evidenceId().toString()).isPresent()) {
                throw new IllegalStateException("QA evidence already exists: " + evidence.evidenceId());
            }
            TaskBoardEvidenceEntity saved = context.save(TaskBoardEvidenceEntity.fromDomain(evidence));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public TaskBoardEvidence reviewEvidence(
            TaskBoardEvidence evidence,
            TaskBoardAuditEntry auditEntry
    ) {
        requireAvailable();
        Objects.requireNonNull(evidence, "evidence");
        requireAudit(auditEntry, Optional.of(evidence.taskId()), requireTaskProjectKey(evidence.taskId()));
        if (evidence.status() == TaskBoardEvidenceStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Evidence review must produce a final state");
        }
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(TASK_LOCK_PREFIX + evidence.taskId());
            context.acquireAdvisoryTransactionLock(EVIDENCE_LOCK_PREFIX + evidence.evidenceId());
            TaskBoardTaskEntity taskEntity = context.find(
                            TaskBoardTaskEntity.class,
                            evidence.taskId().toString(),
                            LockModeType.PESSIMISTIC_WRITE
                    )
                    .orElseThrow(() -> new IllegalStateException("Task disappeared during evidence review"));
            TaskBoardTask task = taskEntity.toDomain();
            TaskBoardEvidenceEntity persisted = context.find(
                            TaskBoardEvidenceEntity.class,
                            evidence.evidenceId().toString(),
                            LockModeType.PESSIMISTIC_WRITE
                    )
                    .orElseThrow(() -> new IllegalStateException("Evidence disappeared during review"));
            if (persisted.toDomain().status() != TaskBoardEvidenceStatus.PENDING_REVIEW) {
                throw new IllegalStateException("Evidence was reviewed concurrently");
            }
            if (evidence.status() == TaskBoardEvidenceStatus.ACCEPTED) {
                if (!evidence.currentFor(TaskBoardTaskFingerprint.compute(task))) {
                    throw new IllegalStateException("Evidence became stale while review was in progress");
                }
            }
            TaskBoardEvidenceEntity saved = context.save(TaskBoardEvidenceEntity.fromDomain(evidence));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            return saved.toDomain();
        });
    }

    @Override
    public TaskBoardTask acceptTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardEvidence evidence,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardVqcContribution> contribution
    ) {
        requireAvailable();
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(expectedUpdatedAt, "expectedUpdatedAt");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(contribution, "contribution");
        if (expectedUpdatedAt.isEmpty()) {
            throw new IllegalArgumentException("Task acceptance requires the task version observed by the reviewer");
        }
        if (task.status() != TaskBoardTaskStatus.DONE || task.qaStatus() != TaskBoardQaStatus.NHV) {
            throw new IllegalArgumentException("Accepted tasks must be DONE and remain NHV");
        }
        Objects.requireNonNull(auditEntry, "auditEntry");
        if (auditEntry.eventType() != TaskBoardAuditEventType.TASK_ACCEPTED) {
            throw new IllegalArgumentException("Task acceptance requires a TASK_ACCEPTED audit entry");
        }
        requireAudit(auditEntry, Optional.of(task.taskId()), task.projectKey());
        if (!evidence.taskId().equals(task.taskId()) || evidence.status() != TaskBoardEvidenceStatus.ACCEPTED) {
            throw new IllegalArgumentException("Task acceptance requires accepted evidence for the same task");
        }
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(TASK_LOCK_PREFIX + task.taskId());
            TaskBoardTaskEntity persisted = context.find(
                            TaskBoardTaskEntity.class,
                            task.taskId().toString(),
                            LockModeType.PESSIMISTIC_WRITE
                    )
                    .orElseThrow(() -> new TaskBoardConcurrentModificationException(
                            "Task disappeared during acceptance: " + task.taskId()
                    ));
            TaskBoardTask current = persisted.toDomain();
            if (!current.updatedAt().equals(expectedUpdatedAt.get())) {
                throw new TaskBoardConcurrentModificationException("Task changed concurrently; reload before accepting");
            }
            if (current.status() != TaskBoardTaskStatus.REVIEW
                    && current.status() != TaskBoardTaskStatus.DONE) {
                throw new TaskBoardConcurrentModificationException("Task is no longer eligible for acceptance");
            }
            TaskBoardEvidenceEntity persistedEvidence = context.find(
                            TaskBoardEvidenceEntity.class,
                            evidence.evidenceId().toString(),
                            LockModeType.PESSIMISTIC_WRITE
                    )
                    .orElseThrow(() -> new TaskBoardConcurrentModificationException(
                            "Accepted evidence disappeared during task acceptance"
                    ));
            TaskBoardEvidence currentEvidence = persistedEvidence.toDomain();
            if (currentEvidence.status() != TaskBoardEvidenceStatus.ACCEPTED
                    || !currentEvidence.taskId().equals(task.taskId())
                    || !currentEvidence.currentFor(TaskBoardTaskFingerprint.compute(current))) {
                throw new TaskBoardConcurrentModificationException(
                        "Evidence is no longer accepted for the current task version"
                );
            }

            String acceptedEvidenceReference = "Accepted task using evidence " + currentEvidence.evidenceId();
            boolean alreadyAccepted = context.findNamed(
                            TaskBoardAuditEntryEntity.class,
                            TaskBoardAuditEntryEntity.FIND_BY_TASK,
                            Map.of("taskId", task.taskId().toString()),
                            0
                    ).stream()
                    .map(TaskBoardAuditEntryEntity::toDomain)
                    .anyMatch(event -> event.eventType() == TaskBoardAuditEventType.TASK_ACCEPTED
                            && event.summary().startsWith(acceptedEvidenceReference));
            if (alreadyAccepted
                    && current.status() == TaskBoardTaskStatus.DONE
                    && current.qaStatus() == TaskBoardQaStatus.NHV) {
                return current;
            }

            TaskBoardTaskEntity saved = context.save(TaskBoardTaskEntity.fromDomain(task));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
            if (contribution.isPresent()) {
                TaskBoardVqcContribution vqc = contribution.get();
                if (!vqc.taskId().equals(task.taskId())
                        || vqc.contributorAccountId().isEmpty()
                        || !vqc.contributorAccountId().get().equals(currentEvidence.accountId())
                        || !vqc.contributorMinecraftUuid().equals(Optional.ofNullable(currentEvidence.submitterMinecraftUuid()))
                        || !current.qaQcEligible()) {
                    throw new IllegalArgumentException("VQC contribution must belong to the accepted task evidence submitter");
                }
                UUID accountId = vqc.contributorAccountId().orElseThrow();
                context.acquireAdvisoryTransactionLock(VQC_LOCK_PREFIX + task.taskId() + ":" + accountId);
                boolean alreadyAwarded = context.findOneNamed(
                        TaskBoardVqcContributionEntity.class,
                        TaskBoardVqcContributionEntity.FIND_BY_TASK_ACCOUNT,
                        Map.of("taskId", task.taskId().toString(), "accountId", accountId.toString())
                ).isPresent();
                if (!alreadyAwarded) {
                    context.save(TaskBoardVqcContributionEntity.fromDomain(vqc));
                }
            }
            return saved.toDomain();
        });
    }

    @Override
    public List<TaskBoardAuditEntry> findAuditEntries(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardAuditEntryEntity.class,
                        TaskBoardAuditEntryEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardAuditEntryEntity::toDomain)
                .toList();
    }

    @Override
    public List<TaskBoardVqcContribution> findVqcContributions(UUID accountId) {
        requireAvailable();
        Objects.requireNonNull(accountId, "accountId");
        return database.entities().findNamed(
                        TaskBoardVqcContributionEntity.class,
                        TaskBoardVqcContributionEntity.FIND_BY_ACCOUNT,
                        Map.of("accountId", accountId.toString()),
                        0
                ).stream()
                .map(TaskBoardVqcContributionEntity::toDomain)
                .toList();
    }

    private String requireTaskProjectKey(UUID taskId) {
        return findTask(taskId)
                .map(TaskBoardTask::projectKey)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId));
    }

    private void requireAudit(TaskBoardAuditEntry auditEntry, Optional<UUID> taskId, String projectKey) {
        Objects.requireNonNull(auditEntry, "auditEntry");
        if (!auditEntry.taskId().equals(taskId) || !auditEntry.projectKey().equals(projectKey)) {
            throw new IllegalArgumentException("Audit entry does not match the mutation scope");
        }
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
        }
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
