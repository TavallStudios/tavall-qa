package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Durable GitHub slug history for a numeric repository/issue identity. */
@Entity
@Table(name = "task_board_external_reference_alias")
@NamedQuery(name = TaskBoardExternalReferenceAliasEntity.FIND_BY_REFERENCE,
        query = """
                SELECT alias FROM TaskBoardExternalReferenceAliasEntity alias
                WHERE alias.referenceId = :referenceId ORDER BY alias.aliasKey
                """)
public class TaskBoardExternalReferenceAliasEntity {
    // Legacy upserts and reference reconciliation must see one durable identity decision.
    public static final String WRITE_LOCK = "tavall-qa:external-reference-map";
    public static final String FIND_BY_REFERENCE = "TaskBoardExternalReferenceAliasEntity.findByReference";
    private static final Pattern GITHUB_ISSUE = Pattern.compile(
            "(?:https://github\\.com/([a-z0-9_.-]+/[a-z0-9_.-]+)/issues/([1-9][0-9]*)"
                    + "|github/([a-z0-9_.-]+/[a-z0-9_.-]+)/issues/([1-9][0-9]*)"
                    + "|github:([a-z0-9_.-]+/[a-z0-9_.-]+)#([1-9][0-9]*))");

    @Id
    @Column(name = "alias_key", length = 255, nullable = false)
    private String aliasKey;

    @Column(name = "reference_id", length = 36, nullable = false)
    private String referenceId;

    protected TaskBoardExternalReferenceAliasEntity() {}

    public TaskBoardExternalReferenceAliasEntity(String aliasKey, String referenceId) {
        this.aliasKey = aliasKey;
        this.referenceId = referenceId;
    }

    public String aliasKey() { return aliasKey; }

    public String referenceId() { return referenceId; }

    public static Optional<String> githubAlias(String value) {
        var match = GITHUB_ISSUE.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (!match.matches()) return Optional.empty();
        for (int group = 1; group <= 5; group += 2) {
            if (match.group(group) != null) {
                String alias = "github/" + match.group(group) + "/issues/" + match.group(group + 1);
                return alias.length() <= 255 ? Optional.of(alias) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
