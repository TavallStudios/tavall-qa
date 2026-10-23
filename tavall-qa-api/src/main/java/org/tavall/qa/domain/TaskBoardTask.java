package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskBoardTask(
        UUID taskId,
        String projectKey,
        String sectionKey,
        String title,
        String publicTitle,
        String description,
        String publicSummary,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        TaskBoardAudience audience,
        int weight,
        boolean hostValidationRequired,
        String sourceType,
        String sourceReference,
        boolean qaQcEligible,
        int qaRevision,
        UUID createdByAccountId,
        UUID createdByMinecraftUUID,
        UUID verifiedByAccountId,
        UUID verifiedByMinecraftUUID,
        UUID verifiedEvidenceId,
        Instant humanVerifiedAt,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public TaskBoardTask(
            UUID taskId,
            String projectKey,
            String sectionKey,
            String title,
            String publicTitle,
            String description,
            String publicSummary,
            TaskBoardTaskStatus status,
            TaskBoardTaskPriority priority,
            TaskBoardQaStatus qaStatus,
            TaskBoardAudience audience,
            int weight,
            boolean hostValidationRequired,
            String sourceType,
            String sourceReference,
            boolean qaQcEligible,
            UUID createdByAccountId,
            UUID createdByMinecraftUUID,
            UUID verifiedByAccountId,
            UUID verifiedByMinecraftUUID,
            UUID verifiedEvidenceId,
            Instant humanVerifiedAt,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                taskId,
                projectKey,
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
                0,
                createdByAccountId,
                createdByMinecraftUUID,
                verifiedByAccountId,
                verifiedByMinecraftUUID,
                verifiedEvidenceId,
                humanVerifiedAt,
                sortOrder,
                createdAt,
                updatedAt
        );
    }

    public TaskBoardTask {
        Objects.requireNonNull(taskId, "taskId");
        projectKey = requireText(projectKey, "projectKey");
        sectionKey = requireText(sectionKey, "sectionKey");
        title = requireText(title, "title");
        publicTitle = normalize(publicTitle);
        description = normalize(description);
        publicSummary = normalize(publicSummary);
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(qaStatus, "qaStatus");
        Objects.requireNonNull(audience, "audience");
        weight = Math.max(1, weight);
        if (qaRevision < 0) {
            throw new IllegalArgumentException("qaRevision must not be negative");
        }
        sourceType = normalize(sourceType);
        sourceReference = normalize(sourceReference);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (qaStatus == TaskBoardQaStatus.HUMAN_VERIFIED
                && (verifiedByAccountId == null
                || verifiedByMinecraftUUID == null
                || verifiedEvidenceId == null
                || humanVerifiedAt == null)) {
            throw new IllegalArgumentException(
                    "HUMAN_VERIFIED tasks require an Account, linked Minecraft identity, accepted evidence, and timestamp"
            );
        }
        if (qaStatus != TaskBoardQaStatus.HUMAN_VERIFIED
                && (verifiedByAccountId != null
                || verifiedByMinecraftUUID != null
                || verifiedEvidenceId != null
                || humanVerifiedAt != null)) {
            throw new IllegalArgumentException(
                    "Only HUMAN_VERIFIED tasks may retain verification evidence"
            );
        }
    }

    public boolean humanVerified() {
        return qaStatus == TaskBoardQaStatus.HUMAN_VERIFIED;
    }

    public boolean publicVisible() {
        return audience == TaskBoardAudience.PUBLIC
                && status != TaskBoardTaskStatus.CANCELLED;
    }

    public boolean completed() {
        return status == TaskBoardTaskStatus.DONE;
    }

    public String publicDisplayTitle() {
        return publicTitle.isBlank() ? title : publicTitle;
    }

    public String publicDisplaySummary() {
        return publicSummary.isBlank() ? description : publicSummary;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
