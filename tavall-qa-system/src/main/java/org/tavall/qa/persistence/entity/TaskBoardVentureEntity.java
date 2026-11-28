package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardVenture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "task_board_venture")
@NamedQuery(
        name = TaskBoardVentureEntity.FIND_ALL,
        query = "SELECT entity FROM TaskBoardVentureEntity entity ORDER BY entity.slug"
)
public class TaskBoardVentureEntity {
    public static final String FIND_ALL = "TaskBoardVentureEntity.findAll";

    @Id
    @Column(name = "slug", nullable = false, length = 96)
    private String slug;

    @Column(name = "source_task_key", nullable = false, unique = true, length = 255)
    private String sourceTaskKey;

    @Column(name = "purpose", nullable = false, columnDefinition = "text")
    private String purpose;

    @Column(name = "owner_repos_json", nullable = false, columnDefinition = "text")
    private String ownerReposJson;

    @Column(name = "stage", nullable = false, length = 96)
    private String stage;

    @Column(name = "live_urls_json", nullable = false, columnDefinition = "text")
    private String liveUrlsJson;

    @Column(name = "top_blocker", nullable = false, columnDefinition = "text")
    private String topBlocker;

    @Column(name = "goal_family_ref", length = 255)
    private String goalFamilyRef;

    @Column(name = "display_name", nullable = false, columnDefinition = "text")
    private String displayName;

    @Column(name = "source_task_id", length = 36)
    private String sourceTaskId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskBoardVentureEntity() {
    }

    public TaskBoardVenture toDomain() {
        return new TaskBoardVenture(slug, sourceTaskKey, purpose, readStringList(ownerReposJson), stage,
                readStringList(liveUrlsJson), topBlocker, goalFamilyRef, displayName,
                sourceTaskId == null ? null : UUID.fromString(sourceTaskId), createdAt, updatedAt);
    }

    private static List<String> readStringList(String value) {
        try {
            return org.tavall.qa.mapping.TaskBoardVentureMapper.readStringList(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Persisted venture list is invalid", exception);
        }
    }
}
