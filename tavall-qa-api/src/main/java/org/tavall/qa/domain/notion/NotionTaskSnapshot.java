package org.tavall.qa.domain.notion;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Typed Notion task snapshot delivered by an authorized agent during sync ingestion.
 */
public record NotionTaskSnapshot(
        String workspaceId,
        String databaseId,
        String pageId,
        String pageUrl,
        Instant lastEditedTime,
        String title,
        String status,
        String executionOwner,
        Instant dueDate,
        String currentStep,
        String checklistProgress,
        String project,
        String domain,
        List<String> domainTags,
        boolean blocked,
        String blockerSummary,
        boolean evidenceComplete,
        String linearId,
        String linearParentUrl,
        boolean archived,
        boolean deleted
) {
    public NotionTaskSnapshot {
        databaseId = requireText(databaseId, "databaseId");
        pageId = requireText(pageId, "pageId");
        pageUrl = requireText(pageUrl, "pageUrl");
        Objects.requireNonNull(lastEditedTime, "lastEditedTime");
        title = title == null ? "" : title.trim();
        status = status == null ? "" : status.trim();
        executionOwner = executionOwner == null ? "" : executionOwner.trim();
        currentStep = currentStep == null ? "" : currentStep.trim();
        checklistProgress = checklistProgress == null ? "" : checklistProgress.trim();
        project = project == null ? "" : project.trim();
        domain = domain == null ? "" : domain.trim();
        domainTags = domainTags == null ? List.of() : List.copyOf(domainTags);
        blockerSummary = blockerSummary == null ? "" : blockerSummary.trim();
        linearId = linearId == null ? "" : linearId.trim();
        linearParentUrl = linearParentUrl == null ? "" : linearParentUrl.trim();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
