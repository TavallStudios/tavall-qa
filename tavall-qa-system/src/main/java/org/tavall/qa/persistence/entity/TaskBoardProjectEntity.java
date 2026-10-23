package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardProjectStatus;

import java.time.Instant;

@Entity
@Table(name = "task_board_project")
@NamedQuery(
        name = TaskBoardProjectEntity.FIND_ALL,
        query = """
                SELECT entity
                FROM TaskBoardProjectEntity entity
                ORDER BY entity.sortOrder, entity.displayName
                """
)
public class TaskBoardProjectEntity {
    public static final String FIND_ALL = "TaskBoardProjectEntity.findAll";

    @Id
    @Column(name = "project_key", nullable = false, length = 96)
    private String projectKey;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false, length = 32)
    private TaskBoardProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 32)
    private TaskBoardAudience audience;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskBoardProjectEntity() {
    }

    private TaskBoardProjectEntity(TaskBoardProject project) {
        projectKey = project.projectKey();
        displayName = project.displayName();
        description = project.description();
        status = project.status();
        audience = project.audience();
        sortOrder = project.sortOrder();
        createdAt = project.createdAt();
        updatedAt = project.updatedAt();
    }

    public static TaskBoardProjectEntity fromDomain(TaskBoardProject project) {
        return new TaskBoardProjectEntity(project);
    }

    public TaskBoardProject toDomain() {
        return new TaskBoardProject(
                projectKey,
                displayName,
                description,
                status,
                audience,
                sortOrder,
                createdAt,
                updatedAt
        );
    }
}
