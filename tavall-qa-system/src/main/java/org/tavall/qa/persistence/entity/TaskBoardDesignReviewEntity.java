package org.tavall.qa.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardDesignReview;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Durable value mapping; Tavall Database owns discovery, transactions and lifecycle. */
@Entity
@Table(name = "task_board_design_review")
public class TaskBoardDesignReviewEntity {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<TaskBoardDesignCandidate>> CANDIDATES = new TypeReference<>() { };

    @Id @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;
    @Column(name = "candidates_json", nullable = false, columnDefinition = "text")
    private String candidatesJson;
    @Column(name = "revision", nullable = false)
    private int revision;
    @Column(name = "registered_by_account_id", nullable = false, length = 36)
    private String registeredByAccountId;
    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;
    @Column(name = "winner_key", length = 1)
    private String winnerKey;
    @Column(name = "decided_by_account_id", length = 36)
    private String decidedByAccountId;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "decision_note", nullable = false, length = 4000)
    private String note;

    protected TaskBoardDesignReviewEntity() { }

    public static TaskBoardDesignReviewEntity fromDomain(TaskBoardDesignReview review) {
        TaskBoardDesignReviewEntity entity = new TaskBoardDesignReviewEntity();
        entity.taskId = review.taskId().toString();
        try {
            entity.candidatesJson = JSON.writeValueAsString(review.candidates());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Design candidates cannot be encoded", exception);
        }
        entity.revision = review.revision();
        entity.registeredByAccountId = review.registeredByAccountId().toString();
        entity.registeredAt = review.registeredAt().truncatedTo(ChronoUnit.MICROS);
        entity.winnerKey = review.winnerKey();
        entity.decidedByAccountId = review.decidedByAccountId() == null ? null : review.decidedByAccountId().toString();
        entity.decidedAt = review.decidedAt() == null ? null : review.decidedAt().truncatedTo(ChronoUnit.MICROS);
        entity.note = review.note();
        return entity;
    }

    public TaskBoardDesignReview toDomain() {
        try {
            return new TaskBoardDesignReview(UUID.fromString(taskId), JSON.readValue(candidatesJson, CANDIDATES), revision,
                    UUID.fromString(registeredByAccountId), registeredAt, winnerKey,
                    decidedByAccountId == null ? null : UUID.fromString(decidedByAccountId), decidedAt, note);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored design candidates do not match their schema", exception);
        }
    }
}
