package org.tavall.qa.cli.github;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests of the REST-only GitHub CLI reader seam; no GitHub or network call is made. */
final class GhCliGithubIssueReaderTest {

    private static final String ISSUES_JSON = """
            [
              {"number": 197, "title": "Typed CLI", "body": "Body text", "state": "open",
               "state_reason": null, "labels": [{"name": "priority:p0"}, {"name": "cli"}],
               "html_url": "https://github.com/TavallStudios/tavall-ai/issues/197"},
              {"number": 198, "title": "A pull request", "state": "open",
               "pull_request": {"url": "https://api.github.com/repos/TavallStudios/tavall-ai/pulls/198"}},
              {"number": 199, "title": "Closed not planned", "body": null, "state": "closed",
               "state_reason": "not_planned", "labels": ["docs"],
               "html_url": "https://github.com/TavallStudios/tavall-ai/issues/199"},
              {"title": "Missing number", "state": "open"}
            ]
            """;

    @Test
    void parseIssuesExcludesPullRequestsAndMalformedEntries() {
        List<QaGithubIssue> issues = GhCliGithubIssueReader.parseIssues("TavallStudios", "tavall-ai", ISSUES_JSON);

        assertEquals(2, issues.size(), "the pull request entry and the entry without a number must be excluded");
        assertEquals(List.of(197, 199), issues.stream().map(QaGithubIssue::number).toList());
    }

    @Test
    void parseIssuesMapsRestFieldsIntoTheTypedRecord() {
        List<QaGithubIssue> issues = GhCliGithubIssueReader.parseIssues("TavallStudios", "tavall-ai", ISSUES_JSON);

        QaGithubIssue open = issues.get(0);
        assertEquals("TavallStudios", open.owner());
        assertEquals("tavall-ai", open.repository());
        assertEquals("Typed CLI", open.title());
        assertEquals("Body text", open.body());
        assertEquals("open", open.state());
        assertNull(open.stateReason());
        assertEquals(List.of("priority:p0", "cli"), open.labels());
        assertEquals("https://github.com/TavallStudios/tavall-ai/issues/197", open.htmlUrl());

        QaGithubIssue closed = issues.get(1);
        assertEquals("closed", closed.state());
        assertEquals("not_planned", closed.stateReason());
        assertEquals("", closed.body(), "a null body must normalize to an empty body");
        assertEquals(List.of("docs"), closed.labels());
    }

    @Test
    void issueExternalKeyAndSourceReferenceAreCanonical() {
        QaGithubIssue issue = GhCliGithubIssueReader.parseIssues(
                "TavallStudios", "tavall-docs", "[{\"number\": 12, \"title\": \"Docs\", \"state\": \"open\"}]").get(0);

        assertEquals("github/TavallStudios/tavall-docs/issues/12", issue.externalKey());
        assertEquals("https://github.com/TavallStudios/tavall-docs/issues/12", issue.sourceReference(),
                "the public GitHub URL is the fallback when html_url is absent");
    }

    @Test
    void parseIssuesRejectsMalformedPayloads() {
        assertThrows(QaGithubReaderException.class,
                () -> GhCliGithubIssueReader.parseIssues("TavallStudios", "tavall-ai", "{not json"));
        assertThrows(QaGithubReaderException.class,
                () -> GhCliGithubIssueReader.parseIssues("TavallStudios", "tavall-ai",
                        "{\"message\":\"Not Found\"}"));
        assertThrows(QaGithubReaderException.class,
                () -> GhCliGithubIssueReader.parseIssues("TavallStudios", "tavall-ai", ""));
    }

    @Test
    void restPathUsesTheRestIssuesEndpointWithPagination() {
        QaGithubIssueQuery query = new QaGithubIssueQuery("TavallStudios", "tavall-ai", "open", 100);

        String path = query.restPath(2);

        assertEquals("repos/TavallStudios/tavall-ai/issues?state=open&per_page=100&page=2", path);
        assertFalse(path.toLowerCase().contains("graphql"), "the reader must only use REST endpoints");
    }

    @Test
    void queryRejectsInvalidOwnerRepositoryStateAndLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("Tavall Studios", "tavall-ai", "open", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("TavallStudios", "tavall ai", "open", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("TavallStudios", "tavall-ai", "draft", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("TavallStudios", "tavall-ai", "open", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("TavallStudios", "tavall-ai", "open", 1001));
        assertThrows(IllegalArgumentException.class,
                () -> new QaGithubIssueQuery("TavallStudios", "tavall-ai", "open", 100).restPath(0));
    }

    @Test
    void missingGitHubCliFailsWithATypedReaderError() {
        GhCliGithubIssueReader reader = new GhCliGithubIssueReader("gh-tavall-qa37-missing", Duration.ofSeconds(5));

        QaGithubReaderException failure = assertThrows(QaGithubReaderException.class,
                () -> reader.readIssues(new QaGithubIssueQuery("TavallStudios", "tavall-ai", "open", 100)));

        assertTrue(failure.getMessage().contains("could not be started"), failure.getMessage());
    }
}
