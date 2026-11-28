package org.tavall.qa.cli.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.qa.cli.client.QaCliSecretRedactor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * {@link IGithubIssueReader} backed by the authenticated GitHub CLI.
 *
 * <p>Every call runs {@code gh api} against the GitHub REST issues endpoint
 * ({@code repos/<owner>/<repo>/issues?state=<state>&per_page=100&page=<n>}). GraphQL is never used,
 * because the REST issues payload exposes {@code state_reason} and {@code pull_request} directly.
 * Pages are requested until a short page arrives or {@code limit} issues have been collected.
 * Pull requests are filtered out, since the REST issues endpoint returns them alongside issues.</p>
 */
public final class GhCliGithubIssueReader implements IGithubIssueReader {

    public static final String GITHUB_CLI = "gh";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);
    public static final String TOKEN_ENVIRONMENT = "GH_TOKEN";
    public static final String GITHUB_TOKEN_ENVIRONMENT = "GITHUB_TOKEN";

    private static final int ERROR_DETAIL_LIMIT = 300;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String executable;
    private final Duration timeout;

    public GhCliGithubIssueReader() {
        this(GITHUB_CLI, DEFAULT_TIMEOUT);
    }

    GhCliGithubIssueReader(String executable, Duration timeout) {
        this.executable = Objects.requireNonNull(executable, "executable");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public List<QaGithubIssue> readIssues(QaGithubIssueQuery query) {
        Objects.requireNonNull(query, "query");
        List<QaGithubIssue> issues = new ArrayList<>();
        for (int page = 1; issues.size() < query.limit(); page++) {
            JsonNode payload = parsePage(execute(query.restPath(page)));
            for (QaGithubIssue issue : parseIssues(query.owner(), query.repository(), payload)) {
                if (issues.size() >= query.limit()) {
                    break;
                }
                issues.add(issue);
            }
            if (payload.size() < QaGithubIssueQuery.PAGE_SIZE) {
                break;
            }
        }
        return List.copyOf(issues);
    }

    /**
     * Parses one GitHub REST issues page, dropping pull requests and malformed entries.
     *
     * @param owner repository owner echoed into every issue
     * @param repository repository name echoed into every issue
     * @param json raw {@code gh api} response body for a single page
     * @return immutable issues in payload order
     */
    public static List<QaGithubIssue> parseIssues(String owner, String repository, String json) {
        return parseIssues(owner, repository, parsePage(json));
    }

    private static JsonNode parsePage(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json == null ? "" : json);
        } catch (JsonProcessingException invalidJson) {
            throw new QaGithubReaderException("GitHub CLI issues response was not valid JSON", invalidJson);
        }
        if (root == null || !root.isArray()) {
            throw new QaGithubReaderException("GitHub CLI issues response was not a JSON array");
        }
        return root;
    }

    private static List<QaGithubIssue> parseIssues(String owner, String repository, JsonNode root) {
        List<QaGithubIssue> issues = new ArrayList<>();
        for (JsonNode node : root) {
            if (!node.isObject() || node.has("pull_request")) {
                // The REST issues endpoint returns pull requests; they are not QA tasks.
                continue;
            }
            int number = node.path("number").asInt(0);
            if (number <= 0) {
                continue;
            }
            issues.add(new QaGithubIssue(
                    owner,
                    repository,
                    number,
                    node.path("title").asText(""),
                    node.path("body").asText(""),
                    node.path("state").asText(""),
                    node.hasNonNull("state_reason") ? node.get("state_reason").asText() : null,
                    labels(node.path("labels")),
                    node.path("html_url").asText("")));
        }
        return List.copyOf(issues);
    }

    private static List<String> labels(JsonNode labelsNode) {
        if (!labelsNode.isArray()) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (JsonNode label : labelsNode) {
            String name = label.isObject() ? label.path("name").asText("") : label.asText("");
            String normalized = name.trim();
            if (!normalized.isEmpty()) {
                labels.add(normalized);
            }
        }
        return List.copyOf(labels);
    }

    private String execute(String restPath) {
        Process process;
        try {
            process = new ProcessBuilder(executable, "api", "--method", "GET", restPath).start();
        } catch (IOException startFailure) {
            throw new QaGithubReaderException(
                    "GitHub CLI '" + executable + "' could not be started (" + startFailure.getClass().getSimpleName()
                            + "); install gh and authenticate it before running qa github sync",
                    startFailure);
        }
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        Thread errorDrain = Thread.ofVirtual().start(() -> drain(process.getErrorStream(), errorOutput));
        try {
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new QaGithubReaderException(
                        "GitHub CLI request timed out after " + timeout.toSeconds() + "s: " + restPath);
            }
            errorDrain.join(Duration.ofSeconds(5).toMillis());
            if (process.exitValue() != 0) {
                throw new QaGithubReaderException("GitHub CLI request failed (exit " + process.exitValue() + "): "
                        + restPath + ": " + redact(errorOutput.toString(StandardCharsets.UTF_8)));
            }
            return new String(output, StandardCharsets.UTF_8);
        } catch (IOException ioFailure) {
            process.destroyForcibly();
            throw new QaGithubReaderException(
                    "GitHub CLI request failed: " + restPath + ": " + ioFailure.getClass().getSimpleName(), ioFailure);
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new QaGithubReaderException("GitHub CLI request was interrupted: " + restPath, interrupted);
        }
    }

    private static void drain(InputStream errorStream, ByteArrayOutputStream sink) {
        try (InputStream stream = errorStream) {
            stream.transferTo(sink);
        } catch (IOException ignored) {
            // stderr is diagnostic only; the process exit value owns the failure decision.
        }
    }

    private static String redact(String detail) {
        String redacted = QaCliSecretRedactor.redact(
                detail.trim(),
                System.getenv(TOKEN_ENVIRONMENT),
                System.getenv(GITHUB_TOKEN_ENVIRONMENT));
        return redacted.length() <= ERROR_DETAIL_LIMIT
                ? redacted
                : redacted.substring(0, ERROR_DETAIL_LIMIT);
    }
}
