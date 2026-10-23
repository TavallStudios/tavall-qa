package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardVqcContribution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class UnavailableTaskBoardDataHandler implements ITaskBoardDataHandler {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<TaskBoardProject> findProjects() {
        return List.of();
    }

    @Override
    public Optional<TaskBoardProject> findProject(String projectKey) {
        return Optional.empty();
    }

    @Override
    public TaskBoardProject saveProject(TaskBoardProject project, TaskBoardAuditEntry auditEntry) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardTask> findTasks(String projectKey) {
        return List.of();
    }

    @Override
    public Optional<TaskBoardTask> findTask(UUID taskId) {
        return Optional.empty();
    }

    @Override
    public TaskBoardTask saveTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardAuditEntry auditEntry
    ) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardAction> findActions(UUID taskId) {
        return List.of();
    }

    @Override
    public TaskBoardAction saveAction(TaskBoardAction action, TaskBoardAuditEntry auditEntry) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardComment> findComments(UUID taskId) {
        return List.of();
    }

    @Override
    public TaskBoardComment saveComment(TaskBoardComment comment, TaskBoardAuditEntry auditEntry) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardAssignment> findAssignments(UUID taskId) {
        return List.of();
    }

    @Override
    public List<TaskBoardAssignment> findAssignmentsForAccount(UUID accountId) {
        return List.of();
    }

    @Override
    public Optional<TaskBoardAssignment> findAssignment(UUID assignmentId) {
        return Optional.empty();
    }

    @Override
    public TaskBoardAssignment saveAssignment(TaskBoardAssignment assignment, TaskBoardAuditEntry auditEntry) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardEvidence> findEvidenceForTask(UUID taskId) {
        return List.of();
    }

    @Override
    public Optional<TaskBoardEvidence> findEvidenceById(UUID evidenceId) {
        return Optional.empty();
    }

    @Override
    public TaskBoardEvidence saveEvidence(TaskBoardEvidence evidence, TaskBoardAuditEntry auditEntry) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public TaskBoardEvidence reviewEvidence(
            TaskBoardEvidence evidence,
            TaskBoardAuditEntry auditEntry
    ) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public TaskBoardTask acceptTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardEvidence evidence,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardVqcContribution> contribution
    ) {
        throw new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardAuditEntry> findAuditEntries(UUID taskId) {
        return List.of();
    }

    @Override
    public List<TaskBoardVqcContribution> findVqcContributions(UUID accountId) {
        return List.of();
    }
}
