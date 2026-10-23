package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardVqcContribution;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface ITaskBoardDataHandler {
    boolean isAvailable();

    List<TaskBoardProject> findProjects();

    Optional<TaskBoardProject> findProject(String projectKey);

    TaskBoardProject saveProject(TaskBoardProject project, TaskBoardAuditEntry auditEntry);

    List<TaskBoardTask> findTasks(String projectKey);

    Optional<TaskBoardTask> findTask(UUID taskId);

    TaskBoardTask saveTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardAuditEntry auditEntry
    );

    List<TaskBoardAction> findActions(UUID taskId);

    TaskBoardAction saveAction(TaskBoardAction action, TaskBoardAuditEntry auditEntry);

    List<TaskBoardComment> findComments(UUID taskId);

    TaskBoardComment saveComment(TaskBoardComment comment, TaskBoardAuditEntry auditEntry);

    List<TaskBoardAssignment> findAssignments(UUID taskId);

    List<TaskBoardAssignment> findAssignmentsForAccount(UUID accountId);

    Optional<TaskBoardAssignment> findAssignment(UUID assignmentId);

    TaskBoardAssignment saveAssignment(TaskBoardAssignment assignment, TaskBoardAuditEntry auditEntry);

    List<TaskBoardEvidence> findEvidenceForTask(UUID taskId);

    Optional<TaskBoardEvidence> findEvidenceById(UUID evidenceId);

    TaskBoardEvidence saveEvidence(TaskBoardEvidence evidence, TaskBoardAuditEntry auditEntry);

    TaskBoardEvidence reviewEvidence(TaskBoardEvidence evidence, TaskBoardAuditEntry auditEntry);

    TaskBoardTask acceptTask(
            TaskBoardTask task,
            Optional<Instant> expectedUpdatedAt,
            TaskBoardEvidence evidence,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardVqcContribution> contribution
    );

    List<TaskBoardAuditEntry> findAuditEntries(UUID taskId);

    List<TaskBoardVqcContribution> findVqcContributions(UUID accountId);
}
