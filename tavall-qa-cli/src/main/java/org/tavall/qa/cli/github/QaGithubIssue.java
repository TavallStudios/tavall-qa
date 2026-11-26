package org.tavall.qa.cli.github;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Typed GitHub issue read through {@link IGithubIssueReader}.
 *
 * <p>Only public repository data enters this record: credentials, tokens and request headers never
 * reach it, so it is safe to report in CLI output.</p>
 */
public record QaGithubIssue(
        String owner,
        String repository,
        int number,
        String title,
        String body,
        String state,
        String stateReason,
        List<String> labels,
        String htmlUrl) {

    public static final String EXTERNAL_KEY_PREFIX = "github/";

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    public QaGithubIssue {
        owner = requireName(owner, "owner");
        repository = requireName(repository, "repository");
        if (number <= 0) {
            throw new IllegalArgumentException("number must be a positive GitHub issue number");
        }
        title = normalizeText(title);
        body = normalizeText(body);
        state = normalizeText(state).toLowerCase(Locale.ROOT);
        stateReason = normalizeOptional(stateReason);
        labels = labels == null ? List.of() : List.copyOf(labels);
        htmlUrl = normalizeText(htmlUrl);
    }

    /** Canonical QA external key, e.g. {@code github/TavallStudios/tavall-ai/issues/197}. */
    public String externalKey() {
        return EXTERNAL_KEY_PREFIX + owner + "/" + repository + "/issues/" + number;
    }

    /** Canonical task source reference; falls back to the public GitHub URL when {@code gh} omits it. */
    public String sourceReference() {
        return htmlUrl.isBlank()
                ? "https://github.com/" + owner + "/" + repository + "/issues/" + number
                : htmlUrl;
    }

    /** Validates a GitHub owner or repository name before it is embedded in a REST API path. */
    public static boolean isValidName(String candidate) {
        return candidate != null && NAME_PATTERN.matcher(candidate.trim()).matches();
    }

    private static String requireName(String value, String field) {
        String normalized = normalizeText(value);
        if (!isValidName(normalized)) {
            throw new IllegalArgumentException(field + " is not a valid GitHub name: '" + normalized + "'");
        }
        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
