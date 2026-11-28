package org.tavall.qa.handler;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceIngestResult;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.handler.ITaskBoardExternalReferenceHandler;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExternalReferenceDataHandler;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceIngestRequest;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Privileged QA owner boundary for external identities and conflict review. */
@DelegatesTo(ITaskBoardExternalReferenceHandler.class)
public final class TaskBoardExternalReferenceHandler extends GenerationOwnedDependencyAccess
        implements ITaskBoardExternalReferenceHandler,
        DependencyAccess<ITaskBoardExternalReferenceDataHandler, ITaskBoardDataHandler, Clock> {

    private ITaskBoardExternalReferenceDataHandler externalReferences() {
        return getInstance().taskBoardExternalReferenceDataHandler();
    }

    private ITaskBoardDataHandler taskBoardDataHandler() {
        return getInstance().taskBoardDataHandler();
    }

    private Clock clock() {
        return getInstance().clock();
    }

    @Override
    public TaskBoardExternalReferenceIngestResult ingest(
            QaSubjectContext subject, TaskBoardExternalReferenceIngestRequest request) {
        requireOwner(subject);
        requireAvailable();
        Objects.requireNonNull(request, "request");
        taskBoardDataHandler().findProject(request.projectKey())
                .orElseThrow(() -> new IllegalArgumentException("Task Board project not found: " + request.projectKey()));
        return externalReferences().ingest(request, new TaskBoardAuditEntry(UUID.randomUUID(), Optional.empty(),
                request.projectKey(), subject.accountId(), subject.linkedMinecraftUuid().orElse(null),
                TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED, "Ingest owner references", clock().instant()));
    }

    @Override
    public Optional<TaskBoardExternalReference> findGitHubReference(QaSubjectContext subject, String alias) {
        requireOwner(subject);
        requireAvailable();
        return externalReferences().findGitHubReference(alias);
    }

    @Override
    public List<String> findGitHubAliases(QaSubjectContext subject, TaskBoardExternalReferenceKey key) {
        requireOwner(subject);
        requireAvailable();
        return externalReferences().findGitHubAliases(key);
    }

    @Override
    public List<TaskBoardExternalReferenceConflict> findConflictHistory(QaSubjectContext subject, UUID taskId) {
        requireOwner(subject);
        requireAvailable();
        requireTask(taskId);
        return externalReferences().findConflictHistory(taskId);
    }

    @Override
    public TaskBoardExternalReferenceMutationResult registerOrReconcile(
            QaSubjectContext subject,
            TaskBoardExternalReferenceMutationRequest request
    ) {
        Objects.requireNonNull(request, "request");
        requireOwner(subject);
        requireAvailable();
        TaskBoardTask task = requireTask(request.taskId());
        TaskBoardAuditEntry audit = audit(subject, task, TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED,
                "Reconcile external reference " + request.key().stableKey());
        return externalReferences().registerOrReconcile(request, audit);
    }

    @Override
    public Optional<TaskBoardExternalReference> findReference(QaSubjectContext subject, TaskBoardExternalReferenceKey key) {
        Objects.requireNonNull(key, "key");
        requireOwner(subject);
        requireAvailable();
        return externalReferences().findReference(key);
    }

    @Override
    public List<TaskBoardExternalReference> findReferencesForTask(QaSubjectContext subject, UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        requireOwner(subject);
        requireAvailable();
        requireTask(taskId);
        return externalReferences().findReferencesForTask(taskId);
    }

    @Override
    public List<TaskBoardExternalReferenceConflict> findOpenConflicts(QaSubjectContext subject, Optional<UUID> taskId) {
        Objects.requireNonNull(taskId, "taskId");
        requireOwner(subject);
        requireAvailable();
        taskId.ifPresent(this::requireTask);
        return externalReferences().findOpenConflicts(taskId);
    }

    @Override
    public TaskBoardExternalReferenceConflict resolveConflict(
            QaSubjectContext subject,
            TaskBoardExternalReferenceConflictResolutionRequest request
    ) {
        Objects.requireNonNull(request, "request");
        requireOwner(subject);
        requireAvailable();
        TaskBoardExternalReferenceConflict conflict = externalReferences().findConflict(request.conflictId())
                .orElseThrow(() -> new IllegalArgumentException("External reference conflict not found: " + request.conflictId()));
        TaskBoardTask task = requireTask(conflict.requestedTaskId());
        TaskBoardAuditEntry audit = audit(subject, task, TaskBoardAuditEventType.EXTERNAL_REFERENCE_RESOLVED,
                "Resolve external reference conflict " + conflict.conflictId());
        return externalReferences().resolveConflict(request, subject.accountId().orElseThrow(), audit);
    }

    private TaskBoardTask requireTask(UUID taskId) {
        return taskBoardDataHandler().findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId));
    }

    private TaskBoardAuditEntry audit(QaSubjectContext subject, TaskBoardTask task,
            TaskBoardAuditEventType type, String summary) {
        return new TaskBoardAuditEntry(UUID.randomUUID(), Optional.of(task.taskId()), task.projectKey(),
                Optional.of(subject.accountId().orElseThrow()), subject.linkedMinecraftUuid().orElse(null),
                type, summary, clock().instant());
    }

    private void requireOwner(QaSubjectContext subject) {
        Objects.requireNonNull(subject, "subject");
        if (!subject.isAccountActive() || !subject.hasQaServiceContext()) {
            throw new SecurityException("An active Tavall Account with the Web service binding is required");
        }
        if (!subject.canManageTasks()) {
            throw new SecurityException("Task management capability required for external references");
        }
    }

    private void requireAvailable() {
        if (!externalReferences().isAvailable() || !taskBoardDataHandler().isAvailable()) {
            throw new TaskBoardStorageUnavailableException("QA Task Board storage is unavailable");
        }
    }
}
