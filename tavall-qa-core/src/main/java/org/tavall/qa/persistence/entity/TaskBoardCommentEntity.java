package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardCommentType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_board_comment")
@NamedQuery(
        name = TaskBoardCommentEntity.FIND_BY_TASK,
        query = """
                SELECT entity
                FROM TaskBoardCommentEntity entity
                WHERE entity.taskId = :taskId
                ORDER BY entity.createdAt, entity.commentId
                """
)
public class TaskBoardCommentEntity {
    public static final String FIND_BY_TASK =
            "TaskBoardCommentEntity.findByTask";

    @Id
    @Column(name = "comment_id", nullable = false, length = 36)
    private String commentId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "parent_comment_id", length = 36)
    private String parentCommentId;

    @Column(name = "author_minecraft_uuid", length = 36)
    private String authorMinecraftUUID;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 32)
    private TaskBoardCommentType type;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskBoardCommentEntity() {
    }

    private TaskBoardCommentEntity(TaskBoardComment comment) {
        commentId = comment.commentId().toString();
        taskId = comment.taskId().toString();
        parentCommentId = uuidString(comment.parentCommentId());
        authorMinecraftUUID = uuidString(comment.authorMinecraftUUID());
        type = comment.type();
        body = comment.body();
        createdAt = comment.createdAt();
        updatedAt = comment.updatedAt();
    }

    public static TaskBoardCommentEntity fromDomain(TaskBoardComment comment) {
        return new TaskBoardCommentEntity(comment);
    }

    public TaskBoardComment toDomain() {
        return new TaskBoardComment(
                UUID.fromString(commentId),
                UUID.fromString(taskId),
                uuid(parentCommentId),
                uuid(authorMinecraftUUID),
                type,
                body,
                createdAt,
                updatedAt
        );
    }

    private static String uuidString(UUID value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
