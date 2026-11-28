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
    private static final TypeReference<List<String>> REFERENCES = new TypeReference<>() { };

    @Id @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;
    @Column(name = "surface", nullable = false, length = 200)
    private String surface;
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

    @Column(name = "ai_reviewer_model", length = 128)
    private String aiReviewerModel;
    @Column(name = "ai_reviewer_account_id", length = 36)
    private String aiReviewerAccountId;
    @Column(name = "ai_reviewer_agent_identity", length = 255)
    private String aiReviewerAgentIdentity;
    @Column(name = "ai_reviewed_at")
    private Instant aiReviewedAt;
    @Column(name = "ai_candidate_key", length = 1)
    private String aiCandidateKey;
    @Column(name = "ai_rationale", columnDefinition = "text")
    private String aiRationale;
    @Column(name = "ai_evidence_refs", nullable = false, columnDefinition = "text")
    private String aiEvidenceRefs;
    @Column(name = "ai_artifact_refs", nullable = false, columnDefinition = "text")
    private String aiArtifactRefs;
    @Column(name = "ai_screenshot_artifact_ref", columnDefinition = "text")
    private String aiScreenshotArtifactRef;

    protected TaskBoardDesignReviewEntity() { }

    public static TaskBoardDesignReviewEntity fromDomain(TaskBoardDesignReview review) {
        TaskBoardDesignReviewEntity entity = new TaskBoardDesignReviewEntity();
        entity.taskId = review.taskId().toString();
        entity.surface = review.surface();
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
        entity.aiReviewerModel = review.aiReviewerModel();
        entity.aiReviewerAccountId = review.aiReviewerAccountId() == null ? null : review.aiReviewerAccountId().toString();
        entity.aiReviewerAgentIdentity = review.aiReviewerAgentIdentity();
        entity.aiReviewedAt = review.aiReviewedAt() == null ? null : review.aiReviewedAt().truncatedTo(ChronoUnit.MICROS);
        entity.aiCandidateKey = review.aiCandidateKey();
        entity.aiRationale = review.aiRationale();
        try {
            entity.aiEvidenceRefs = JSON.writeValueAsString(review.aiEvidenceRefs());
            entity.aiArtifactRefs = JSON.writeValueAsString(review.aiArtifactRefs());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI design review refs cannot be serialized", exception);
        }
        entity.aiScreenshotArtifactRef = review.aiScreenshotArtifactRef();
        return entity;
    }

    public TaskBoardDesignReview toDomain() {
        try {
            List<String> evidenceRefs = aiEvidenceRefs == null || aiEvidenceRefs.isBlank() ? List.of()
                    : JSON.readValue(aiEvidenceRefs, REFERENCES);
            List<String> artifactRefs = aiArtifactRefs == null || aiArtifactRefs.isBlank() ? List.of()
                    : JSON.readValue(aiArtifactRefs, REFERENCES);
            return new TaskBoardDesignReview(UUID.fromString(taskId), surface, JSON.readValue(candidatesJson, CANDIDATES), revision,
                    UUID.fromString(registeredByAccountId), registeredAt, winnerKey,
                    decidedByAccountId == null ? null : UUID.fromString(decidedByAccountId), decidedAt, note,
                    aiReviewerModel, aiReviewerAccountId == null ? null : UUID.fromString(aiReviewerAccountId),
                    aiReviewerAgentIdentity, aiReviewedAt, aiCandidateKey, aiRationale,
                    evidenceRefs, artifactRefs, aiScreenshotArtifactRef);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored design review payload does not match its schema", exception);
        }
    }
}
