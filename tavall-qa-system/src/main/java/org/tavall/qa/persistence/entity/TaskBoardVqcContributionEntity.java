package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardVqcContribution;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "qa_vqc_contribution")
@NamedQueries({
        @NamedQuery(
                name = TaskBoardVqcContributionEntity.FIND_BY_ACCOUNT,
                query = "SELECT entity FROM TaskBoardVqcContributionEntity entity WHERE entity.contributorAccountId = :accountId ORDER BY entity.awardedAt"
        ),
        @NamedQuery(
                name = TaskBoardVqcContributionEntity.FIND_BY_TASK_ACCOUNT,
                query = "SELECT entity FROM TaskBoardVqcContributionEntity entity WHERE entity.taskId = :taskId AND entity.contributorAccountId = :accountId"
        )
})
public class TaskBoardVqcContributionEntity {
    public static final String FIND_BY_ACCOUNT = "TaskBoardVqcContributionEntity.findByAccount";
    public static final String FIND_BY_TASK_ACCOUNT = "TaskBoardVqcContributionEntity.findByTaskAccount";

    @Id
    @Column(name = "contribution_id", nullable = false, length = 36)
    private String contributionId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "contributor_account_id", length = 36)
    private String contributorAccountId;

    @Column(name = "contributor_minecraft_uuid", length = 36)
    private String contributorMinecraftUuid;

    @Column(name = "base_vqc", nullable = false)
    private int baseVqc;

    @Column(name = "bonus_vqc", nullable = false)
    private int bonusVqc;

    @Column(name = "bonus_approved", nullable = false)
    private boolean bonusApproved;

    @Column(name = "notes", nullable = false, columnDefinition = "text")
    private String notes;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt;

    protected TaskBoardVqcContributionEntity() {
    }

    private TaskBoardVqcContributionEntity(TaskBoardVqcContribution contribution) {
        contributionId = contribution.contributionId().toString();
        taskId = contribution.taskId().toString();
        contributorAccountId = contribution.contributorAccountId().map(UUID::toString).orElse(null);
        contributorMinecraftUuid = contribution.contributorMinecraftUuid().map(UUID::toString).orElse(null);
        baseVqc = contribution.baseVqc();
        bonusVqc = contribution.bonusVqc();
        bonusApproved = contribution.bonusApproved();
        notes = contribution.notes();
        awardedAt = contribution.awardedAt();
    }

    public static TaskBoardVqcContributionEntity fromDomain(TaskBoardVqcContribution contribution) {
        return new TaskBoardVqcContributionEntity(contribution);
    }

    public TaskBoardVqcContribution toDomain() {
        return new TaskBoardVqcContribution(
                UUID.fromString(contributionId),
                UUID.fromString(taskId),
                Optional.ofNullable(uuid(contributorAccountId)),
                Optional.ofNullable(uuid(contributorMinecraftUuid)),
                baseVqc,
                bonusVqc,
                bonusApproved,
                notes,
                awardedAt
        );
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
