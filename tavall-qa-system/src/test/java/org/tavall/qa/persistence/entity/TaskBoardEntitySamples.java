package org.tavall.qa.persistence.entity;

import org.tavall.qa.domain.*;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.domain.execution.*;
import org.tavall.qa.domain.external.*;
import org.tavall.qa.domain.notion.NotionTaskSyncState;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class TaskBoardEntitySamples {
    static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    static final String PROJECT = "project-novus";
    static final UUID TASK_ID = id(1);
    static final UUID ACCOUNT_ID = id(2);

    private TaskBoardEntitySamples() { }

    static UUID id(long value) { return new UUID(0, value); }

    static TaskBoardProject project() {
        return new TaskBoardProject(PROJECT, "Project Novus", "QA project", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 10, NOW, NOW);
    }

    static TaskBoardTask task() {
        return new TaskBoardTask(TASK_ID, PROJECT, "combat", "Review combat", "Combat review",
                "Check combat behavior", "Public summary", TaskBoardTaskStatus.READY,
                TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 3,
                false, "manual", "qa://combat", true, 2, ACCOUNT_ID, null, null, null, null, null,
                4, NOW, NOW);
    }

    static TaskBoardAction action() {
        return new TaskBoardAction(id(3), TASK_ID, TaskBoardActionType.ADMIN_ROUTE,
                "Open QA", "/admin/qa", 1, NOW);
    }

    static TaskBoardAssignment assignment() {
        return new TaskBoardAssignment(id(4), id(5), TASK_ID, ACCOUNT_ID, QaCapability.MANAGE_TASKS,
                QaEnvironmentGrant.DEV, Optional.of(QaClass.TECHNICAL), Optional.of(QaTier.TIER_III),
                id(6), "QA ownership", NOW, null);
    }

    static TaskBoardAuditEntry auditEntry() {
        return new TaskBoardAuditEntry(id(7), Optional.of(TASK_ID), PROJECT, Optional.of(ACCOUNT_ID), id(8),
                TaskBoardAuditEventType.TASK_UPDATED, "Task updated", NOW);
    }

    static TaskBoardComment comment() {
        return new TaskBoardComment(id(9), TASK_ID, null, ACCOUNT_ID, id(10), TaskBoardCommentType.COMMENT,
                "Ready for review", NOW, NOW);
    }

    static TaskBoardEvidence evidence() {
        return new TaskBoardEvidence(id(11), TASK_ID, ACCOUNT_ID, id(12), Set.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_III), QaEnvironmentGrant.DEV, "build-42", "combat smoke",
                "No regression", "No regression", "evidence://combat/42", "Combat passed", true,
                "a".repeat(64), TaskBoardEvidenceStatus.PENDING_REVIEW, null, "", NOW, null);
    }

    static TaskBoardVqcContribution contribution() {
        return new TaskBoardVqcContribution(id(13), TASK_ID, Optional.of(ACCOUNT_ID), Optional.of(id(14)),
                1, 0, false, "Accepted QA work", NOW);
    }

    static NotionTaskSyncState notionState() {
        return new NotionTaskSyncState(id(15), TASK_ID, "database-qa", "page-combat",
                "https://notion.example/page-combat", NOW, 4, 3, false, "", false, false, NOW, NOW);
    }

    static TaskBoardExecutionSourceData executionSource() {
        return new TaskBoardExecutionSourceData("TavallStudios/tavall-qa", "working/qa", "a".repeat(40),
                "b".repeat(64));
    }

    static TaskBoardExecutionCheckpointData checkpoint() {
        return new TaskBoardExecutionCheckpointData(id(16), TASK_ID, id(17), id(18), 1, "a".repeat(64),
                executionSource(), "b".repeat(64), "codex-neo", TaskBoardExecutionStatus.RUNNING, ACCOUNT_ID, NOW);
    }

    static TaskBoardExecutionCheckpointPayloadData payload() {
        String digest = "c".repeat(64);
        return new TaskBoardExecutionCheckpointPayloadData(1, "Resume the review", List.of("Keep task identity"),
                new TaskBoardExecutionDiffData(digest, null, URI.create("tavall-storage://artifacts/sha256/" + digest)),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of("qa-worker"), List.of(), List.of(),
                List.of(), List.of("qa.read"), List.of(), "Inspect the last checkpoint");
    }

    static TaskBoardExecutionLeaseData lease() {
        return new TaskBoardExecutionLeaseData(TASK_ID, id(19), id(20), ACCOUNT_ID, 2, 1,
                "a".repeat(64), NOW.plusSeconds(600));
    }

    static TaskBoardExternalReferenceKey externalKey() {
        return new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.GITHUB_ISSUE, "12345", "67890");
    }

    static TaskBoardExternalReference externalReference() {
        return new TaskBoardExternalReference(id(21), TASK_ID, externalKey(), "TAV-90",
                "https://github.com/TavallStudios/tavall-qa/issues/90", TaskBoardExternalReferenceSourceStatus.IN_PROGRESS,
                "rev-4", "rev-3", "cursor-4", "d".repeat(64), TaskBoardExternalReferenceSyncState.CONNECTED,
                4, NOW, NOW, NOW, NOW);
    }

    static TaskBoardExternalReferenceConflict externalConflict() {
        return new TaskBoardExternalReferenceConflict(id(22), externalKey(), Optional.of(id(21)), Optional.empty(),
                TASK_ID, "TAV-90", "https://github.com/TavallStudios/tavall-qa/issues/90",
                TaskBoardExternalReferenceSourceStatus.IN_PROGRESS, "rev-5", "cursor-5", "e".repeat(64), NOW,
                4, "Incoming revision requires review", TaskBoardExternalReferenceConflictStatus.OPEN,
                Optional.empty(), Optional.empty(), NOW, Optional.empty());
    }
}
