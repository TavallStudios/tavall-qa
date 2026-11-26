package org.tavall.qa.cli.github;

import java.util.List;

/**
 * Typed read seam for GitHub issues consumed by {@code qa github sync}.
 *
 * <p>Implementations return issues only: pull requests listed by the GitHub issues endpoint are
 * filtered out before they reach callers. Readers never mutate GitHub state.</p>
 */
public interface IGithubIssueReader {

    /**
     * Reads up to {@link QaGithubIssueQuery#limit()} issues for one repository.
     *
     * @param query typed owner, repository, state filter and per-repository limit
     * @return immutable issues in GitHub page order, never {@code null}
     */
    List<QaGithubIssue> readIssues(QaGithubIssueQuery query);
}
