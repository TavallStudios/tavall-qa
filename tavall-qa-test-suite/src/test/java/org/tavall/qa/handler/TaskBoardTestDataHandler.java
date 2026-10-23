package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardVqcContribution;
import org.tavall.qa.persistence.ITaskBoardDataHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class TaskBoardTestDataHandler implements ITaskBoardDataHandler {
    private final Map<String, TaskBoardProject> projects = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardTask> tasks = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardAction> actions = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardComment> comments = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardAssignment> assignments = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardEvidence> evidence = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardVqcContribution> contributions = new LinkedHashMap<>();
    private final List<TaskBoardAuditEntry> audit = new ArrayList<>();

    @Override public boolean isAvailable() { return true; }
    @Override public List<TaskBoardProject> findProjects() { return List.copyOf(projects.values()); }
    @Override public Optional<TaskBoardProject> findProject(String projectKey) { return Optional.ofNullable(projects.get(projectKey)); }

    @Override
    public TaskBoardProject saveProject(TaskBoardProject project, TaskBoardAuditEntry auditEntry) {
        projects.put(project.projectKey(), project);
        audit.add(auditEntry);
        return project;
    }

    @Override
    public List<TaskBoardTask> findTasks(String projectKey) {
        return tasks.values().stream().filter(task -> task.projectKey().equals(projectKey)).toList();
    }

    @Override public Optional<TaskBoardTask> findTask(UUID taskId) { return Optional.ofNullable(tasks.get(taskId)); }

    @Override
    public TaskBoardTask saveTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardAuditEntry auditEntry
    ) {
        TaskBoardTask current = tasks.get(task.taskId());
        if (expectedUpdatedAt.isEmpty() && current != null) {
            throw new IllegalStateException("Task already exists");
        }
        if (expectedUpdatedAt.isPresent()
                && (current == null || !current.updatedAt().equals(expectedUpdatedAt.get()))) {
            throw new IllegalStateException("Task changed concurrently");
        }
        tasks.put(task.taskId(), task);
        audit.add(auditEntry);
        return task;
    }

    @Override
    public List<TaskBoardAction> findActions(UUID taskId) {
        return actions.values().stream().filter(action -> action.taskId().equals(taskId)).toList();
    }

    @Override
    public TaskBoardAction saveAction(TaskBoardAction action, TaskBoardAuditEntry auditEntry) {
        actions.put(action.actionId(), action);
        audit.add(auditEntry);
        return action;
    }

    @Override
    public List<TaskBoardComment> findComments(UUID taskId) {
        return comments.values().stream().filter(comment -> comment.taskId().equals(taskId)).toList();
    }

    @Override
    public TaskBoardComment saveComment(TaskBoardComment comment, TaskBoardAuditEntry auditEntry) {
        comments.put(comment.commentId(), comment);
        audit.add(auditEntry);
        return comment;
    }

    @Override
    public List<TaskBoardAssignment> findAssignments(UUID taskId) {
        return assignments.values().stream().filter(assignment -> assignment.taskId().equals(taskId)).toList();
    }

    @Override
    public List<TaskBoardAssignment> findAssignmentsForAccount(UUID accountId) {
        return assignments.values().stream().filter(assignment -> assignment.accountId().equals(accountId)).toList();
    }

    @Override public Optional<TaskBoardAssignment> findAssignment(UUID assignmentId) {
        return Optional.ofNullable(assignments.get(assignmentId));
    }

    @Override
    public TaskBoardAssignment saveAssignment(TaskBoardAssignment assignment, TaskBoardAuditEntry auditEntry) {
        assignments.put(assignment.assignmentId(), assignment);
        audit.add(auditEntry);
        return assignment;
    }

    @Override
    public List<TaskBoardEvidence> findEvidenceForTask(UUID taskId) {
        return evidence.values().stream().filter(item -> item.taskId().equals(taskId)).toList();
    }

    @Override public Optional<TaskBoardEvidence> findEvidenceById(UUID evidenceId) {
        return Optional.ofNullable(evidence.get(evidenceId));
    }

    @Override
    public TaskBoardEvidence saveEvidence(TaskBoardEvidence item, TaskBoardAuditEntry auditEntry) {
        evidence.put(item.evidenceId(), item);
        audit.add(auditEntry);
        return item;
    }

    @Override
    public TaskBoardEvidence reviewEvidence(
            TaskBoardEvidence item,
            TaskBoardAuditEntry auditEntry
    ) {
        TaskBoardEvidence current = evidence.get(item.evidenceId());
        if (current == null || current.status() != TaskBoardEvidenceStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Evidence was reviewed concurrently");
        }
        evidence.put(item.evidenceId(), item);
        audit.add(auditEntry);
        return item;
    }

    @Override
    public TaskBoardTask acceptTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardEvidence acceptedEvidence,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardVqcContribution> contribution
    ) {
        TaskBoardTask current = tasks.get(task.taskId());
        if (current == null || expectedUpdatedAt.isEmpty()
                || !current.updatedAt().equals(expectedUpdatedAt.get())) {
            throw new IllegalStateException("Task changed concurrently");
        }
        TaskBoardEvidence persistedEvidence = evidence.get(acceptedEvidence.evidenceId());
        if (persistedEvidence == null || persistedEvidence.status() != TaskBoardEvidenceStatus.ACCEPTED
                || !persistedEvidence.taskId().equals(task.taskId())
                || !persistedEvidence.currentFor(org.tavall.qa.domain.TaskBoardTaskFingerprint.compute(current))) {
            throw new IllegalStateException("Accepted evidence is stale or does not belong to the task");
        }
        tasks.put(task.taskId(), task);
        audit.add(auditEntry);
        contribution.ifPresent(candidate -> {
            boolean alreadyAwarded = contributions.values().stream().anyMatch(existing ->
                    existing.taskId().equals(candidate.taskId())
                            && existing.contributorAccountId().equals(candidate.contributorAccountId()));
            if (!alreadyAwarded) {
                contributions.put(candidate.contributionId(), candidate);
            }
        });
        return task;
    }

    @Override
    public List<TaskBoardAuditEntry> findAuditEntries(UUID taskId) {
        return audit.stream().filter(entry -> entry.taskId().filter(taskId::equals).isPresent()).toList();
    }

    @Override
    public List<TaskBoardVqcContribution> findVqcContributions(UUID accountId) {
        return contributions.values().stream()
                .filter(contribution -> contribution.contributorAccountId().filter(accountId::equals).isPresent())
                .toList();
    }
}
