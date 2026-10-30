package org.tavall.qa.domain.notion;

import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.Map;
import java.util.Objects;

/**
 * Typed per-database mapping configuration translating Notion database property names
 * to QA Board fields (title, status mapping, owner, due date, description/context,
 * project/domain, blocked/blocker, evidence complete).
 */
public record NotionDatabaseTaskMapping(
        String databaseId,
        String databaseName,
        String defaultProjectKey,
        String titleProperty,
        String statusProperty,
        Map<String, TaskBoardTaskStatus> statusMapping,
        Map<TaskBoardTaskStatus, String> outboundStatusMapping,
        String ownerProperty,
        String dueDateProperty,
        String currentStepProperty,
        String checklistProgressProperty,
        String blockedProperty,
        String blockerSummaryProperty,
        String evidenceCompleteProperty,
        String projectProperty,
        String domainProperty,
        String domainTagsProperty,
        String linearIdProperty,
        String linearParentUrlProperty,
        String lastReconciledProperty
) {
    public NotionDatabaseTaskMapping {
        Objects.requireNonNull(databaseId, "databaseId");
        Objects.requireNonNull(databaseName, "databaseName");
        Objects.requireNonNull(defaultProjectKey, "defaultProjectKey");
        titleProperty = normalize(titleProperty, "Name");
        statusProperty = normalize(statusProperty, "Status");
        statusMapping = Map.copyOf(statusMapping != null ? statusMapping : defaultStatusMapping());
        outboundStatusMapping = Map.copyOf(outboundStatusMapping != null ? outboundStatusMapping : defaultOutboundStatusMapping());
        ownerProperty = normalize(ownerProperty, "Execution Owner");
        dueDateProperty = normalize(dueDateProperty, "Due date");
        currentStepProperty = normalize(currentStepProperty, "Current Step");
        checklistProgressProperty = normalize(checklistProgressProperty, "Checklist Progress");
        blockedProperty = normalize(blockedProperty, "Blocked");
        blockerSummaryProperty = normalize(blockerSummaryProperty, "Blocker Summary");
        evidenceCompleteProperty = normalize(evidenceCompleteProperty, "Evidence Complete");
        projectProperty = normalize(projectProperty, "Project");
        domainProperty = normalize(domainProperty, "Domain");
        domainTagsProperty = normalize(domainTagsProperty, "Domain Tags");
        linearIdProperty = normalize(linearIdProperty, "Linear ID");
        linearParentUrlProperty = normalize(linearParentUrlProperty, "Linear Parent URL");
        lastReconciledProperty = normalize(lastReconciledProperty, "Last Reconciled");
    }

    public static Map<String, TaskBoardTaskStatus> defaultStatusMapping() {
        return Map.of(
                "Not started", TaskBoardTaskStatus.BACKLOG,
                "Up next", TaskBoardTaskStatus.READY,
                "In progress", TaskBoardTaskStatus.ACTIVE,
                "Done", TaskBoardTaskStatus.DONE
        );
    }

    public static Map<TaskBoardTaskStatus, String> defaultOutboundStatusMapping() {
        return Map.of(
                TaskBoardTaskStatus.BACKLOG, "Not started",
                TaskBoardTaskStatus.READY, "Up next",
                TaskBoardTaskStatus.ACTIVE, "In progress",
                TaskBoardTaskStatus.BLOCKED, "In progress",
                TaskBoardTaskStatus.REVIEW, "In progress",
                TaskBoardTaskStatus.DONE, "Done",
                TaskBoardTaskStatus.CANCELLED, "Done"
        );
    }

    public static NotionDatabaseTaskMapping canonicalPvpTasks() {
        return new NotionDatabaseTaskMapping(
                "3a338458ddfd80eb9bbfcb8bc741e610",
                "Tavall PvP Tasks",
                "tavall-pvp",
                "Name",
                "Status",
                defaultStatusMapping(),
                defaultOutboundStatusMapping(),
                "Execution Owner",
                "Due date",
                "Current Step",
                "Checklist Progress",
                "Blocked",
                "Blocker Summary",
                "Evidence Complete",
                "Project",
                "Domain",
                "Domain Tags",
                "Linear ID",
                "Linear Parent URL",
                "Last Reconciled"
        );
    }

    public static NotionDatabaseTaskMapping canonicalKingdomsTasks() {
        return new NotionDatabaseTaskMapping(
                "b3f34d39223b43378a60531cd7e6fdbf",
                "Tavall Kingdoms Tasks",
                "tavall-kingdoms",
                "Name",
                "Status",
                defaultStatusMapping(),
                defaultOutboundStatusMapping(),
                "Execution Owner",
                "Due date",
                "Current Step",
                "Checklist Progress",
                "Blocked",
                "Blocker Summary",
                "Evidence Complete",
                "Project",
                "Domain",
                "Domain Tags",
                "Linear ID",
                "Linear Parent URL",
                "Last Reconciled"
        );
    }

    public TaskBoardTaskStatus mapInboundStatus(String notionStatus, boolean blocked) {
        if (blocked) {
            return TaskBoardTaskStatus.BLOCKED;
        }
        if (notionStatus == null || notionStatus.isBlank()) {
            return TaskBoardTaskStatus.BACKLOG;
        }
        TaskBoardTaskStatus mapped = statusMapping.get(notionStatus.trim());
        return mapped != null ? mapped : TaskBoardTaskStatus.BACKLOG;
    }

    public String mapOutboundStatus(TaskBoardTaskStatus qaStatus) {
        if (qaStatus == null) {
            return "Not started";
        }
        String mapped = outboundStatusMapping.get(qaStatus);
        return mapped != null ? mapped : "Not started";
    }

    private static String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
