package org.tavall.qa.cli.github;

import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic GitHub issue to QA task mapping used by {@code qa github sync}.
 *
 * <p>Every rule is a pure static function so the mapping can be unit tested without GitHub, the
 * GitHub CLI or the QA API. Mapping never throws for unexpected issue content: unmappable states
 * return an empty {@link Optional} so the caller can report a skip instead of failing the batch.</p>
 */
public final class QaGithubSyncMapping {

    /** Canonical task source type for GitHub issue backed tasks. */
    public static final String SOURCE_TYPE = "github-issue";
    /** Maximum characters copied from an issue body into a task description. */
    public static final int DESCRIPTION_LIMIT = 1500;

    private static final String TRUNCATION_SUFFIX = "...";
    private static final String CLOSED_NOT_PLANNED = "not_planned";
    private static final Pattern TITLE_PRIORITY = Pattern.compile("\\bP([0-4])\\b", Pattern.CASE_INSENSITIVE);

    private static final Map<String, TaskBoardTaskPriority> LABEL_PRIORITIES = Map.ofEntries(
            Map.entry("p0", TaskBoardTaskPriority.CRITICAL),
            Map.entry("priority:p0", TaskBoardTaskPriority.CRITICAL),
            Map.entry("critical", TaskBoardTaskPriority.CRITICAL),
            Map.entry("p1", TaskBoardTaskPriority.HIGH),
            Map.entry("priority:p1", TaskBoardTaskPriority.HIGH),
            Map.entry("high", TaskBoardTaskPriority.HIGH),
            Map.entry("low", TaskBoardTaskPriority.LOW));

    private QaGithubSyncMapping() {
    }

    /**
     * Maps a GitHub issue state to the canonical task status.
     *
     * <p>{@code open} is {@code READY}; {@code closed}/{@code not_planned} is {@code CANCELLED};
     * every other {@code closed} issue is {@code DONE}. Unknown states return empty so the caller
     * records a skip.</p>
     */
    public static Optional<TaskBoardTaskStatus> status(QaGithubIssue issue) {
        return switch (issue.state()) {
            case "open" -> Optional.of(TaskBoardTaskStatus.READY);
            case "closed" -> Optional.of(CLOSED_NOT_PLANNED.equals(issue.stateReason())
                    ? TaskBoardTaskStatus.CANCELLED
                    : TaskBoardTaskStatus.DONE);
            default -> Optional.empty();
        };
    }

    /**
     * Infers task priority from the issue title and labels.
     *
     * <p>{@code P0} in the title, or {@code p0}/{@code priority:p0}/{@code critical} labels are
     * {@code CRITICAL}; {@code P1} or {@code p1}/{@code priority:p1}/{@code high} labels are
     * {@code HIGH}; {@code P3}/{@code P4} or a {@code low} label are {@code LOW}; everything else is
     * {@code NORMAL}. The strongest signal wins when title and labels disagree.</p>
     */
    public static TaskBoardTaskPriority priority(QaGithubIssue issue) {
        TaskBoardTaskPriority strongest = null;
        Matcher matcher = TITLE_PRIORITY.matcher(issue.title());
        while (matcher.find()) {
            strongest = stronger(strongest, switch (matcher.group(1)) {
                case "0" -> TaskBoardTaskPriority.CRITICAL;
                case "1" -> TaskBoardTaskPriority.HIGH;
                case "2" -> TaskBoardTaskPriority.NORMAL;
                default -> TaskBoardTaskPriority.LOW;
            });
        }
        for (String label : issue.labels()) {
            if (label != null) {
                strongest = stronger(strongest, LABEL_PRIORITIES.get(label.trim().toLowerCase(Locale.ROOT)));
            }
        }
        return strongest == null ? TaskBoardTaskPriority.NORMAL : strongest;
    }

    /**
     * Builds a concise, sanitized task description from the issue body.
     *
     * <p>Control characters are removed, blank-line runs and trailing spaces are collapsed, and the
     * excerpt is bounded to {@link #DESCRIPTION_LIMIT} characters. A blank body falls back to the
     * issue title, then to the canonical external key.</p>
     */
    public static String description(QaGithubIssue issue) {
        String body = sanitize(issue.body());
        if (body.isEmpty()) {
            String title = sanitize(issue.title());
            return title.isEmpty() ? issue.externalKey() : bound(title);
        }
        return bound(body);
    }

    /** Canonical QA external key for an issue, e.g. {@code github/TavallStudios/tavall-ai/issues/197}. */
    public static String externalKey(String owner, String repository, int number) {
        return QaGithubIssue.EXTERNAL_KEY_PREFIX + owner + "/" + repository + "/issues/" + number;
    }

    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder stripped = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (codePoint == '\n' || codePoint == '\t' || !Character.isISOControl(codePoint)) {
                stripped.appendCodePoint(codePoint);
            }
        });
        return stripped.toString()
                .replaceAll("[\\t ]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static String bound(String text) {
        if (text.length() <= DESCRIPTION_LIMIT) {
            return text;
        }
        int end = DESCRIPTION_LIMIT - TRUNCATION_SUFFIX.length();
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + TRUNCATION_SUFFIX;
    }

    private static TaskBoardTaskPriority stronger(TaskBoardTaskPriority current, TaskBoardTaskPriority candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return severity(candidate) > severity(current) ? candidate : current;
    }

    private static int severity(TaskBoardTaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> 3;
            case HIGH -> 2;
            case NORMAL -> 1;
            case LOW -> 0;
        };
    }
}
