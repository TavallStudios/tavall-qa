package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardActionCreateRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public final class TaskBoardMutationHandler implements ITaskBoardMutationHandler {
    private static final Pattern PROJECT_KEY = Pattern.compile(
            "^[a-z0-9][a-z0-9-]{0,95}$"
    );
    private static final Pattern SECTION_KEY = Pattern.compile(
            "^[a-z0-9][a-z0-9-]{0,127}$"
    );

    private final ITaskBoardDataHandler dataHandler;
    private final ITaskBoardActionTargetResolver actionTargetResolver;

    public TaskBoardMutationHandler(
            ITaskBoardDataHandler dataHandler,
            ITaskBoardActionTargetResolver actionTargetResolver
    ) {
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.actionTargetResolver = Objects.requireNonNull(
                actionTargetResolver,
                "actionTargetResolver"
        );
    }

    @Override
    public TaskBoardProject createProject(TaskBoardProjectCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAvailable();
        String projectKey = normalizeProjectKey(request.projectKey());
        if (dataHandler.findProject(projectKey).isPresent()) {
            throw new IllegalArgumentException(
                    "Task Board project already exists: " + projectKey
            );
        }
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                projectKey,
                request.displayName(),
                request.description(),
                Objects.requireNonNull(request.status(), "status"),
                Objects.requireNonNull(request.audience(), "audience"),
                request.sortOrder(),
                now,
                now
        );
        return dataHandler.saveProject(project);
    }

    @Override
    public TaskBoardTask createTask(TaskBoardTaskCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAvailable();
        String projectKey = normalizeProjectKey(request.projectKey());
        String sectionKey = normalizeSectionKey(request.sectionKey());
        requireProject(projectKey);
        Instant now = Instant.now();
        TaskBoardTask task = new TaskBoardTask(
                UUID.randomUUID(),
                projectKey,
                sectionKey,
                request.title(),
                request.publicTitle(),
                request.description(),
                request.publicSummary(),
                TaskBoardTaskStatus.BACKLOG,
                Objects.requireNonNull(request.priority(), "priority"),
                TaskBoardQaStatus.NHV,
                Objects.requireNonNull(request.audience(), "audience"),
                request.weight(),
                request.hostValidationRequired(),
                request.sourceType(),
                request.sourceReference(),
                request.actorMinecraftUUID(),
                null,
                null,
                request.sortOrder(),
                now,
                now
        );
        return dataHandler.saveTask(task);
    }

    @Override
    public TaskBoardTask updateTask(TaskBoardTaskUpdateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAvailable();
        TaskBoardTask current = dataHandler.findTask(request.taskId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task Board task not found: " + request.taskId()
                ));
        Instant now = Instant.now();
        TaskBoardQaStatus qaStatus = Objects.requireNonNull(request.qaStatus(), "qaStatus");

        boolean materialChange = !current.title().equals(request.title())
                || !current.description().equals(request.description())
                || !current.sourceReference().equals(request.sourceReference())
                || !current.sourceType().equals(request.sourceType())
                || current.hostValidationRequired() != request.hostValidationRequired()
                || request.status() != TaskBoardTaskStatus.DONE;

        UUID verifiedByMinecraftUUID = null;
        Instant humanVerifiedAt = null;

        if (qaStatus == TaskBoardQaStatus.HUMAN_VERIFIED) {
            if (request.status() != TaskBoardTaskStatus.DONE) {
                throw new IllegalArgumentException(
                        "Only completed tasks can be Human Verified"
                );
            }
            if (materialChange && request.actorMinecraftUUID() == null) {
                // Invalidate prior verification if material changes happened without explicit re-verification
                qaStatus = TaskBoardQaStatus.REVIEW_REQUIRED;
            } else {
                verifiedByMinecraftUUID = requireHumanVerifier(
                        request.actorMinecraftUUID() != null
                                ? request.actorMinecraftUUID()
                                : current.verifiedByMinecraftUUID()
                );
                humanVerifiedAt = current.humanVerified()
                        && verifiedByMinecraftUUID.equals(current.verifiedByMinecraftUUID())
                        && !materialChange
                        ? current.humanVerifiedAt()
                        : now;
            }
        }

        TaskBoardTask updated = new TaskBoardTask(
                current.taskId(),
                current.projectKey(),
                normalizeSectionKey(request.sectionKey()),
                request.title(),
                request.publicTitle(),
                request.description(),
                request.publicSummary(),
                Objects.requireNonNull(request.status(), "status"),
                Objects.requireNonNull(request.priority(), "priority"),
                qaStatus,
                Objects.requireNonNull(request.audience(), "audience"),
                request.weight(),
                request.hostValidationRequired(),
                request.sourceType(),
                request.sourceReference(),
                current.createdByMinecraftUUID(),
                verifiedByMinecraftUUID,
                humanVerifiedAt,
                request.sortOrder(),
                current.createdAt(),
                now
        );
        return dataHandler.saveTask(updated);
    }

    @Override
    public TaskBoardTask verifyTask(UUID taskId, UUID verifierMinecraftUuid, Instant verifiedAt) {
        Objects.requireNonNull(taskId, "taskId");
        requireAvailable();
        TaskBoardTask current = dataHandler.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId));
        if (current.status() != TaskBoardTaskStatus.DONE) {
            throw new IllegalArgumentException("Only DONE tasks can be Human Verified");
        }
        UUID verifier = requireHumanVerifier(verifierMinecraftUuid);
        Instant timestamp = verifiedAt != null ? verifiedAt : Instant.now();
        TaskBoardTask verified = new TaskBoardTask(
                current.taskId(),
                current.projectKey(),
                current.sectionKey(),
                current.title(),
                current.publicTitle(),
                current.description(),
                current.publicSummary(),
                current.status(),
                current.priority(),
                TaskBoardQaStatus.HUMAN_VERIFIED,
                current.audience(),
                current.weight(),
                current.hostValidationRequired(),
                current.sourceType(),
                current.sourceReference(),
                current.createdByMinecraftUUID(),
                verifier,
                timestamp,
                current.sortOrder(),
                current.createdAt(),
                Instant.now()
        );
        return dataHandler.saveTask(verified);
    }

    @Override
    public TaskBoardTask rejectTask(UUID taskId, String reason) {
        Objects.requireNonNull(taskId, "taskId");
        requireAvailable();
        TaskBoardTask current = dataHandler.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId));
        Instant now = Instant.now();
        TaskBoardTask rejected = new TaskBoardTask(
                current.taskId(),
                current.projectKey(),
                current.sectionKey(),
                current.title(),
                current.publicTitle(),
                current.description(),
                current.publicSummary(),
                TaskBoardTaskStatus.REVIEW,
                current.priority(),
                TaskBoardQaStatus.REVIEW_REQUIRED,
                current.audience(),
                current.weight(),
                current.hostValidationRequired(),
                current.sourceType(),
                current.sourceReference(),
                current.createdByMinecraftUUID(),
                null,
                null,
                current.sortOrder(),
                current.createdAt(),
                now
        );
        TaskBoardTask saved = dataHandler.saveTask(rejected);
        if (reason != null && !reason.isBlank()) {
            addComment(new TaskBoardCommentCreateRequest(
                    taskId,
                    null,
                    null,
                    TaskBoardCommentType.QA_NOTE,
                    "Task rejected: " + reason.trim()
            ));
        }
        return saved;
    }

    @Override
    public TaskBoardAction addAction(TaskBoardActionCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAvailable();
        requireTask(request.taskId());
        String target = actionTargetResolver.normalizeTarget(
                Objects.requireNonNull(request.type(), "type"),
                request.target()
        );
        TaskBoardAction action = new TaskBoardAction(
                UUID.randomUUID(),
                request.taskId(),
                request.type(),
                request.label(),
                target,
                request.sortOrder(),
                Instant.now()
        );
        return dataHandler.saveAction(action);
    }

    @Override
    public TaskBoardComment addComment(TaskBoardCommentCreateRequest request) {
        Objects.requireNonNull(request, "request");
        requireAvailable();
        requireTask(request.taskId());
        validateParentComment(request.taskId(), request.parentCommentId());
        Instant now = Instant.now();
        TaskBoardComment comment = new TaskBoardComment(
                UUID.randomUUID(),
                request.taskId(),
                request.parentCommentId(),
                request.authorMinecraftUUID(),
                Objects.requireNonNull(request.type(), "type"),
                request.body(),
                now,
                now
        );
        return dataHandler.saveComment(comment);
    }

    private void validateParentComment(UUID taskId, UUID parentCommentId) {
        if (parentCommentId == null) {
            return;
        }
        List<TaskBoardComment> comments = dataHandler.findComments(taskId);
        boolean belongsToTask = comments.stream()
                .anyMatch(comment -> comment.commentId().equals(parentCommentId));
        if (!belongsToTask) {
            throw new IllegalArgumentException(
                    "Parent comment does not belong to this Task Board task"
            );
        }
    }

    private UUID requireHumanVerifier(UUID actorMinecraftUUID) {
        if (actorMinecraftUUID == null) {
            throw new IllegalArgumentException(
                    "Human Verified status requires a linked Minecraft identity"
            );
        }
        return actorMinecraftUUID;
    }

    private void requireProject(String projectKey) {
        if (dataHandler.findProject(projectKey).isEmpty()) {
            throw new IllegalArgumentException(
                    "Task Board project not found: " + projectKey
            );
        }
    }

    private void requireTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        if (dataHandler.findTask(taskId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Task Board task not found: " + taskId
            );
        }
    }

    private String normalizeProjectKey(String projectKey) {
        return normalizeKey(projectKey, PROJECT_KEY, "Project");
    }

    private String normalizeSectionKey(String sectionKey) {
        return normalizeKey(sectionKey, SECTION_KEY, "Section");
    }

    private String normalizeKey(String value, Pattern pattern, String label) {
        Objects.requireNonNull(value, label.toLowerCase(Locale.ROOT) + "Key");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    label + " keys use lowercase letters, numbers, and hyphens"
            );
        }
        return normalized;
    }

    private void requireAvailable() {
        if (!dataHandler.isAvailable()) {
            throw new IllegalStateException(
                    "Task Board PostgreSQL storage is unavailable."
            );
        }
    }
}
