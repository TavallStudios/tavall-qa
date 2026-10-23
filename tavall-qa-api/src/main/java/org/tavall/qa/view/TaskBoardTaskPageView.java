package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record TaskBoardTaskPageView(
        UUID taskId,
        String projectKey,
        String projectDisplayName,
        String sectionKey,
        String title,
        String summary,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        TaskBoardAudience audience,
        int weight,
        String sourceType,
        Instant updatedAt,
        boolean publicVisible,
        List<TaskBoardAssignedEvidenceView> assignedEvidence,
        List<TaskBoardPublicEvidenceView> publicEvidence
) {
    public TaskBoardTaskPageView {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(projectDisplayName, "projectDisplayName");
        Objects.requireNonNull(sectionKey, "sectionKey");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(qaStatus, "qaStatus");
        Objects.requireNonNull(audience, "audience");
        sourceType = sourceType == null ? "" : sourceType;
        Objects.requireNonNull(updatedAt, "updatedAt");
        assignedEvidence = List.copyOf(Objects.requireNonNull(assignedEvidence, "assignedEvidence"));
        publicEvidence = List.copyOf(Objects.requireNonNull(publicEvidence, "publicEvidence"));
    }

    public static TaskBoardTaskPageView publicTask(
            String projectDisplayName,
            TaskBoardPublicTaskView task
    ) {
        return new TaskBoardTaskPageView(
                task.taskId(),
                task.projectKey(),
                projectDisplayName,
                task.sectionKey(),
                task.title(),
                task.summary(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                TaskBoardAudience.PUBLIC,
                task.weight(),
                task.sourceType(),
                task.updatedAt(),
                true,
                List.of(),
                task.evidence()
        );
    }

    public static TaskBoardTaskPageView staffTask(TaskBoardTaskView taskView) {
        TaskBoardTask task = taskView.task();
        return new TaskBoardTaskPageView(
                task.taskId(),
                task.projectKey(),
                taskView.project().displayName(),
                task.sectionKey(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                task.audience(),
                task.weight(),
                task.sourceType(),
                task.updatedAt(),
                taskView.project().publicVisible() && task.publicVisible(),
                taskView.evidence().stream()
                        .map(TaskBoardEvidenceView::evidence)
                        .map(evidence -> new TaskBoardAssignedEvidenceView(
                                evidence.evidenceId(),
                                evidence.environment(),
                                evidence.buildReference(),
                                evidence.testCase(),
                                evidence.expectedResult(),
                                evidence.actualResult(),
                                evidence.evidenceReference(),
                                evidence.publicSummary(),
                                evidence.status(),
                        evidence.currentFor(org.tavall.qa.domain.TaskBoardTaskFingerprint.compute(task)),
                                evidence.submittedAt()
                        ))
                        .toList(),
                taskView.evidence().stream()
                        .map(TaskBoardEvidenceView::evidence)
                        .filter(evidence -> evidence.status() == org.tavall.qa.domain.TaskBoardEvidenceStatus.ACCEPTED)
                        .filter(org.tavall.qa.domain.TaskBoardEvidence::publicSummaryApproved)
                        .filter(evidence -> !evidence.publicSummary().isBlank())
                        .map(evidence -> new TaskBoardPublicEvidenceView(
                                evidence.publicSummary(),
                                evidence.environment(),
                                evidence.submittedAt()
                        ))
                        .toList()
        );
    }

    /** Scoped assignment projection without staff identities, destinations, comments, or source references. */
    public static TaskBoardTaskPageView assignedTask(
            String projectDisplayName,
            boolean projectPublicVisible,
            TaskBoardTask task,
            List<TaskBoardAssignedEvidenceView> assignedEvidence,
            List<TaskBoardPublicEvidenceView> publicEvidence
    ) {
        boolean isPublic = projectPublicVisible && task.publicVisible();
        return new TaskBoardTaskPageView(
                task.taskId(),
                task.projectKey(),
                projectDisplayName,
                task.sectionKey(),
                isPublic ? task.publicDisplayTitle() : task.title(),
                isPublic ? task.publicDisplaySummary() : task.description(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                isPublic ? TaskBoardAudience.PUBLIC : task.audience(),
                task.weight(),
                task.sourceType(),
                task.updatedAt(),
                isPublic,
                assignedEvidence,
                publicEvidence
        );
    }
}
