package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "task_board_evidence")
@NamedQuery(
        name = TaskBoardEvidenceEntity.FIND_BY_TASK,
        query = "SELECT entity FROM TaskBoardEvidenceEntity entity WHERE entity.taskId = :taskId ORDER BY entity.submittedAt"
)
public class TaskBoardEvidenceEntity {
    public static final String FIND_BY_TASK = "TaskBoardEvidenceEntity.findByTask";

    @Id
    @Column(name = "evidence_id", nullable = false, length = 36)
    private String evidenceId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "submitter_minecraft_uuid", length = 36)
    private String submitterMinecraftUuid;

    @Column(name = "qa_classes_at_submission", nullable = false, length = 128)
    private String qaClassesAtSubmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "qa_tier_at_submission", length = 16)
    private QaTier qaTierAtSubmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 16)
    private QaEnvironmentGrant environment;

    @Column(name = "build_reference", nullable = false, length = 255)
    private String buildReference;

    @Column(name = "test_case", nullable = false, columnDefinition = "text")
    private String testCase;

    @Column(name = "expected_result", nullable = false, columnDefinition = "text")
    private String expectedResult;

    @Column(name = "actual_result", nullable = false, columnDefinition = "text")
    private String actualResult;

    @Column(name = "evidence_reference", nullable = false, length = 2048)
    private String evidenceReference;

    @Column(name = "public_summary", nullable = false, columnDefinition = "text")
    private String publicSummary;

    @Column(name = "public_summary_approved", nullable = false)
    private boolean publicSummaryApproved;

    @Column(name = "task_fingerprint", nullable = false, length = 64)
    private String taskFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 24)
    private TaskBoardEvidenceStatus status;

    @Column(name = "reviewer_account_id", length = 36)
    private String reviewerAccountId;

    @Column(name = "review_reason", nullable = false, columnDefinition = "text")
    private String reviewReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected TaskBoardEvidenceEntity() {
    }

    private TaskBoardEvidenceEntity(TaskBoardEvidence evidence) {
        evidenceId = evidence.evidenceId().toString();
        taskId = evidence.taskId().toString();
        accountId = evidence.accountId().toString();
        submitterMinecraftUuid = uuidString(evidence.submitterMinecraftUuid());
        qaClassesAtSubmission = evidence.qaClassesAtSubmission().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        qaTierAtSubmission = evidence.qaTierAtSubmission().orElse(null);
        environment = evidence.environment();
        buildReference = evidence.buildReference();
        testCase = evidence.testCase();
        expectedResult = evidence.expectedResult();
        actualResult = evidence.actualResult();
        evidenceReference = evidence.evidenceReference();
        publicSummary = evidence.publicSummary();
        publicSummaryApproved = evidence.publicSummaryApproved();
        taskFingerprint = evidence.taskFingerprint();
        status = evidence.status();
        reviewerAccountId = uuidString(evidence.reviewerAccountId());
        reviewReason = evidence.reviewReason();
        submittedAt = evidence.submittedAt();
        reviewedAt = evidence.reviewedAt();
    }

    public static TaskBoardEvidenceEntity fromDomain(TaskBoardEvidence evidence) {
        return new TaskBoardEvidenceEntity(evidence);
    }

    public TaskBoardEvidence toDomain() {
        Set<QaClass> classes = qaClassesAtSubmission.isBlank()
                ? Set.of()
                : Arrays.stream(qaClassesAtSubmission.split(","))
                .map(QaClass::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        return new TaskBoardEvidence(
                UUID.fromString(evidenceId),
                UUID.fromString(taskId),
                UUID.fromString(accountId),
                uuid(submitterMinecraftUuid),
                classes,
                java.util.Optional.ofNullable(qaTierAtSubmission),
                environment,
                buildReference,
                testCase,
                expectedResult,
                actualResult,
                evidenceReference,
                publicSummary,
                publicSummaryApproved,
                taskFingerprint,
                status,
                uuid(reviewerAccountId),
                reviewReason,
                submittedAt,
                reviewedAt
        );
    }

    private static String uuidString(UUID value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
