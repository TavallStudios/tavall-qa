package org.tavall.qa.cli.github;

import java.util.Locale;
import java.util.Set;

/**
 * Typed request for one repository page run of the GitHub issue reader.
 *
 * <p>{@link #restPath(int)} only ever builds a GitHub REST path: the reader never uses GraphQL.</p>
 */
public record QaGithubIssueQuery(String owner, String repository, String state, int limit) {

    public static final String DEFAULT_STATE = "open";
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;
    public static final int PAGE_SIZE = 100;
    public static final Set<String> STATES = Set.of("open", "closed", "all");

    public QaGithubIssueQuery {
        if (!QaGithubIssue.isValidName(owner)) {
            throw new IllegalArgumentException("owner is not a valid GitHub name: '" + owner + "'");
        }
        if (!QaGithubIssue.isValidName(repository)) {
            throw new IllegalArgumentException("repository is not a valid GitHub name: '" + repository + "'");
        }
        owner = owner.trim();
        repository = repository.trim();
        String normalizedState = state == null ? DEFAULT_STATE : state.trim().toLowerCase(Locale.ROOT);
        if (!STATES.contains(normalizedState)) {
            throw new IllegalArgumentException("state must be one of open, closed, all but was '" + state + "'");
        }
        state = normalizedState;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT + " but was " + limit);
        }
    }

    /** Builds the GitHub REST issues path for one page, e.g. {@code repos/o/r/issues?state=open&per_page=100&page=1}. */
    public String restPath(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be positive");
        }
        return "repos/" + owner + "/" + repository + "/issues"
                + "?state=" + state
                + "&per_page=" + PAGE_SIZE
                + "&page=" + page;
    }
}
