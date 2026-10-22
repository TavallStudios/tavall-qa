package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardActionType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_board_action")
@NamedQuery(
        name = TaskBoardActionEntity.FIND_BY_TASK,
        query = """
                SELECT entity
                FROM TaskBoardActionEntity entity
                WHERE entity.taskId = :taskId
                ORDER BY entity.sortOrder, entity.createdAt
                """
)
public class TaskBoardActionEntity {
    public static final String FIND_BY_TASK =
            "TaskBoardActionEntity.findByTask";

    @Id
    @Column(name = "action_id", nullable = false, length = 36)
    private String actionId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private TaskBoardActionType type;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "target", nullable = false, length = 2048)
    private String target;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskBoardActionEntity() {
    }

    private TaskBoardActionEntity(TaskBoardAction action) {
        actionId = action.actionId().toString();
        taskId = action.taskId().toString();
        type = action.type();
        label = action.label();
        target = action.target();
        sortOrder = action.sortOrder();
        createdAt = action.createdAt();
    }

    public static TaskBoardActionEntity fromDomain(TaskBoardAction action) {
        return new TaskBoardActionEntity(action);
    }

    public TaskBoardAction toDomain() {
        return new TaskBoardAction(
                UUID.fromString(actionId),
                UUID.fromString(taskId),
                type,
                label,
                target,
                sortOrder,
                createdAt
        );
    }
}
