package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.registry.ICLICommandRegistry;
import org.tavall.cli.command.registry.CLICommandRegistry;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignRegisterRequest;
import org.tavall.qa.cli.client.QaCliDesignRegisterResult;
import org.tavall.qa.cli.client.QaCliDesignReviewSnapshot;
import org.tavall.qa.cli.client.QaCliTaskUpsertRequest;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;
import org.tavall.qa.cli.github.IGithubIssueReader;
import org.tavall.qa.cli.github.QaGithubIssue;
import org.tavall.qa.cli.github.QaGithubIssueQuery;
import org.tavall.qa.cli.github.QaGithubReaderException;
import org.tavall.qa.cli.github.QaGithubSyncMapping;
import org.tavall.qa.cli.github.QaGithubSyncResult;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests of {@code qa github sync}: typed mapping, dry run, multi-repository batches and failures. */
final class QaGithubSyncCommandTest {
    private static final String OWNER = "TavallStudios";
    private static final String TAVALL_AI_197 = "github/TavallStudios/tavall-ai/issues/197";
    private static final String TAVALL_AI_198 = "github/TavallStudios/tavall-ai/issues/198";
    private static final String TAVALL_DOCS_12 = "github/TavallStudios/tavall-docs/issues/12";

    private final StubGithubIssueReader reader = new StubGithubIssueReader();
    private final RecordingQaExecutionApiClient api = new RecordingQaExecutionApiClient();

    @BeforeEach
    void registerStubs() {
        DependencyMap map = DependencyMap.getDependencyMap();
        map.clear();
        // Registered before boot/resolution so no test can reach gh or the QA HTTP client.
        map.registerInstance(IGithubIssueReader.class, reader);
        map.registerInstance(IQaExecutionApiClient.class, api);
    }

    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void dryRunReportsPlannedKeysWithoutCallingTheQaApi() {
        reader.issuesByRepository.put("tavall-ai", List.of(
                issue("tavall-ai", 197, "P0: harden CLI", "Body for 197", "open", null, List.of("priority:p0")),
                issue("tavall-ai", 198, "Wave 2 packaging", "", "closed", "completed", List.of())));
        reader.issuesByRepository.put("tavall-docs", List.of(
                issue("tavall-docs", 12, "Retired docs idea", "No longer needed", "closed", "not_planned", List.of())));

        CLICommandResult result = command().execute(input(Map.of(
                "repos", "tavall-ai,tavall-docs",
                "dry-run", Boolean.TRUE)));

        assertTrue(result.successful(), result.message());
        assertEquals(0, result.exitCode());
        assertEquals("true", result.data().get("dryRun"));
        assertEquals("0", result.data().get("created"));
        assertEquals("0", result.data().get("updated"));
        assertEquals("3", result.data().get("scanned"));
        assertEquals(TAVALL_AI_197 + "," + TAVALL_AI_198 + "," + TAVALL_DOCS_12,
                result.data().get("syncedKeys"));
        assertTrue(result.data().get("errors") == null, "a dry run must not report errors");
        assertTrue(result.message().contains("no QA API calls made"), result.message());
        assertTrue(api.upserts.isEmpty(), "a dry run must not call IQaExecutionApiClient.upsertTask");

        assertEquals(List.of("tavall-ai", "tavall-docs"),
                reader.queries.stream().map(QaGithubIssueQuery::repository).toList());
        assertTrue(reader.queries.stream().allMatch(query ->
                OWNER.equals(query.owner())
                        && QaGithubSyncCommand.DEFAULT_STATE.equals(query.state())
                        && query.limit() == QaGithubSyncCommand.DEFAULT_LIMIT));
    }

    @Test
    void multiRepositorySyncUpsertsCanonicalKeysAndDeterministicMappings() {
        reader.issuesByRepository.put("tavall-ai", List.of(
                issue("tavall-ai", 197, "P0: harden CLI", "Body for 197", "open", null, List.of("priority:p0")),
                issue("tavall-ai", 198, "Wave 2 packaging", "", "closed", "completed", List.of())));
        reader.issuesByRepository.put("tavall-docs", List.of(
                issue("tavall-docs", 12, "Retired docs idea", "No longer needed", "closed", "not_planned",
                        List.of("low"))));
        api.existingKeys.add(TAVALL_DOCS_12);

        CLICommandResult result = command().execute(input(Map.of(
                "repos", "tavall-ai,tavall-docs",
                "state", "all",
                "limit", 25,
                "project", "tavall-docs",
                "section", "docs",
                "credential", "/tmp/qa-agent.json")));

        assertTrue(result.successful(), result.message());
        assertEquals("3", result.data().get("scanned"));
        assertEquals("2", result.data().get("created"));
        assertEquals("1", result.data().get("updated"));
        assertEquals("0", result.data().get("skipped"));
        assertEquals("0", result.data().get("failed"));
        assertEquals(TAVALL_AI_197 + "," + TAVALL_AI_198 + "," + TAVALL_DOCS_12,
                result.data().get("syncedKeys"));

        assertEquals(List.of(TAVALL_AI_197, TAVALL_AI_198, TAVALL_DOCS_12),
                api.upserts.stream().map(QaCliTaskUpsertRequest::externalKey).toList());

        QaCliTaskUpsertRequest critical = api.upserts.get(0);
        assertEquals("P0: harden CLI", critical.title());
        assertEquals(TaskBoardTaskStatus.READY, critical.status());
        assertEquals(TaskBoardTaskPriority.CRITICAL, critical.priority());
        assertEquals("Body for 197", critical.description());
        assertEquals("github-issue", critical.sourceType());
        assertEquals("https://github.com/TavallStudios/tavall-ai/issues/197", critical.sourceReference());
        assertEquals("docs", critical.section());
        assertEquals("tavall-docs", critical.project());
        assertNotNull(critical.credentialFile());
        assertEquals("/tmp/qa-agent.json", critical.credentialFile().toString());

        QaCliTaskUpsertRequest done = api.upserts.get(1);
        assertEquals(TaskBoardTaskStatus.DONE, done.status());
        assertEquals(TaskBoardTaskPriority.NORMAL, done.priority());
        assertEquals("Wave 2 packaging", done.description(),
                "a blank issue body falls back to the issue title");

        QaCliTaskUpsertRequest cancelled = api.upserts.get(2);
        assertEquals(TaskBoardTaskStatus.CANCELLED, cancelled.status());
        assertEquals(TaskBoardTaskPriority.LOW, cancelled.priority());

        assertTrue(reader.queries.stream().allMatch(query -> "all".equals(query.state()) && query.limit() == 25));
    }

    @Test
    void perIssueClientFailureIsRecordedWithoutAbortingTheBatch() {
        reader.issuesByRepository.put("tavall-ai", List.of(
                issue("tavall-ai", 197, "First issue", "Body", "open", null, List.of()),
                issue("tavall-ai", 198, "Failing issue", "Body", "open", null, List.of())));
        reader.issuesByRepository.put("tavall-docs", List.of(
                issue("tavall-docs", 12, "Last issue", "Body", "open", null, List.of())));
        api.failingKeys.add(TAVALL_AI_198);

        CLICommandResult result = command().execute(input(Map.of("repos", "tavall-ai,tavall-docs")));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals(QaGithubSyncCommand.FAILURE_REASON, result.failureReason());
        assertEquals("3", result.data().get("scanned"));
        assertEquals("2", result.data().get("created"));
        assertEquals("1", result.data().get("failed"));
        assertEquals(TAVALL_AI_197 + "," + TAVALL_DOCS_12, result.data().get("syncedKeys"));
        assertTrue(result.data().get("errors").contains(TAVALL_AI_198), result.data().get("errors"));
        assertTrue(api.upserts.stream().map(QaCliTaskUpsertRequest::externalKey).toList()
                        .contains(TAVALL_DOCS_12),
                "the batch must continue after an issue failure");
    }

    @Test
    void unsupportedIssueStateIsSkippedAndReported() {
        reader.issuesByRepository.put("tavall-ai", List.of(
                issue("tavall-ai", 197, "Weird state", "Body", "archived", null, List.of())));

        CLICommandResult result = command().execute(input(Map.of("repos", "tavall-ai")));

        assertTrue(result.successful(), result.message());
        assertEquals("1", result.data().get("scanned"));
        assertEquals("1", result.data().get("skipped"));
        assertEquals("0", result.data().get("failed"));
        assertEquals("", result.data().get("syncedKeys"));
        assertTrue(result.data().get("errors").contains("unsupported issue state 'archived'"));
        assertTrue(api.upserts.isEmpty());
    }

    @Test
    void readerFailureMapsToGithubReaderErrorWithoutApiCalls() {
        reader.failure = new QaGithubReaderException("GitHub CLI request failed (exit 4): repos/TavallStudios/"
                + "tavall-ai/issues?state=open");

        CLICommandResult result = command().execute(input(Map.of("repos", "tavall-ai")));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals(QaGithubSyncCommand.READER_FAILURE_REASON, result.failureReason());
        assertTrue(result.message().contains("GitHub CLI request failed"), result.message());
        assertTrue(api.upserts.isEmpty());
    }

    @Test
    void missingRepositoriesFailAsInvalidArguments() {
        CLICommandResult result = command().execute(input(Map.of()));

        assertFalse(result.successful());
        assertEquals(2, result.exitCode());
        assertEquals("INVALID_ARGUMENT", result.failureReason());
        assertTrue(reader.queries.isEmpty(), "argument validation must run before any GitHub read");
    }

    @Test
    void invalidStateFilterFailsAsInvalidArgument() {
        CLICommandResult result = command().execute(input(Map.of("repos", "tavall-ai", "state", "merged")));

        assertFalse(result.successful());
        assertEquals(2, result.exitCode());
        assertEquals("INVALID_ARGUMENT", result.failureReason());
        assertTrue(reader.queries.isEmpty());
    }

    @Test
    void repositoriesAreDeduplicatedAndInvalidNamesRejected() {
        assertEquals(List.of("tavall-ai", "tavall-docs"),
                QaGithubSyncCommand.parseRepositories(" tavall-ai , tavall-docs , tavall-ai ,, "));
        assertThrows(IllegalArgumentException.class, () -> QaGithubSyncCommand.parseRepositories("tavall ai"));
        assertThrows(IllegalArgumentException.class, () -> QaGithubSyncCommand.parseRepositories(" , "));
        assertThrows(IllegalArgumentException.class, () -> QaGithubSyncCommand.parseRepositories(null));
    }

    @Test
    void statusMappingFollowsIssueStateAndReason() {
        assertEquals(Optional.of(TaskBoardTaskStatus.READY),
                QaGithubSyncMapping.status(issue("tavall-ai", 1, "t", "", "open", null, List.of())));
        assertEquals(Optional.of(TaskBoardTaskStatus.DONE),
                QaGithubSyncMapping.status(issue("tavall-ai", 2, "t", "", "closed", "completed", List.of())));
        assertEquals(Optional.of(TaskBoardTaskStatus.DONE),
                QaGithubSyncMapping.status(issue("tavall-ai", 3, "t", "", "closed", null, List.of())));
        assertEquals(Optional.of(TaskBoardTaskStatus.CANCELLED),
                QaGithubSyncMapping.status(issue("tavall-ai", 4, "t", "", "closed", "not_planned", List.of())));
        assertEquals(Optional.empty(),
                QaGithubSyncMapping.status(issue("tavall-ai", 5, "t", "", "reopened", null, List.of())));
    }

    @Test
    void priorityInferenceUsesTitleTokensAndLabels() {
        assertEquals(TaskBoardTaskPriority.CRITICAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 1, "P0: outage", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.CRITICAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 2, "Outage", "", "open", null,
                        List.of("priority:p0"))));
        assertEquals(TaskBoardTaskPriority.CRITICAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 3, "Outage", "", "open", null,
                        List.of("critical"))));
        assertEquals(TaskBoardTaskPriority.HIGH,
                QaGithubSyncMapping.priority(issue("tavall-ai", 4, "P1: slow CLI", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.HIGH,
                QaGithubSyncMapping.priority(issue("tavall-ai", 5, "Slow CLI", "", "open", null,
                        List.of("priority:p1"))));
        assertEquals(TaskBoardTaskPriority.HIGH,
                QaGithubSyncMapping.priority(issue("tavall-ai", 6, "Slow CLI", "", "open", null, List.of("high"))));
        assertEquals(TaskBoardTaskPriority.LOW,
                QaGithubSyncMapping.priority(issue("tavall-ai", 7, "P3: tidy docs", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.LOW,
                QaGithubSyncMapping.priority(issue("tavall-ai", 8, "P4: tidy docs", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.LOW,
                QaGithubSyncMapping.priority(issue("tavall-ai", 9, "Tidy docs", "", "open", null, List.of("low"))));
        assertEquals(TaskBoardTaskPriority.NORMAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 10, "P2: tidy docs", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.NORMAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 11, "Routine work", "", "open", null, List.of())));
        assertEquals(TaskBoardTaskPriority.NORMAL,
                QaGithubSyncMapping.priority(issue("tavall-ai", 12, "Upgrade P10 runtime", "", "open", null, List.of())),
                "P10 is not a priority token");
        assertEquals(TaskBoardTaskPriority.HIGH,
                QaGithubSyncMapping.priority(issue("tavall-ai", 13, "P3: tidy docs", "", "open", null,
                        List.of("high"))),
                "the strongest of title and labels wins");
    }

    @Test
    void descriptionsAreSanitizedAndBounded() {
        String longBody = ("\u0000\u0001 " + "issue body line\n\n\n\n".repeat(200)).repeat(2);
        QaGithubIssue longIssue = issue("tavall-ai", 1, "Title", longBody, "open", null, List.of());

        String description = QaGithubSyncMapping.description(longIssue);

        assertTrue(description.length() <= QaGithubSyncMapping.DESCRIPTION_LIMIT + 3,
                "description length was " + description.length());
        assertTrue(description.endsWith("..."), description.substring(description.length() - 20));
        assertFalse(description.contains("\u0000"));
        assertFalse(description.contains("\n\n\n"), "blank-line runs must be collapsed");

        assertEquals("Title",
                QaGithubSyncMapping.description(issue("tavall-ai", 2, "Title", "   ", "open", null, List.of())));
        assertEquals("github/TavallStudios/tavall-ai/issues/3",
                QaGithubSyncMapping.description(issue("tavall-ai", 3, "  ", "", "open", null, List.of())));
        assertEquals("https://github.com/TavallStudios/tavall-ai/issues/1", longIssue.sourceReference());
        assertEquals("github/TavallStudios/tavall-ai/issues/1",
                QaGithubSyncMapping.externalKey("TavallStudios", "tavall-ai", 1));
    }

    @Test
    void bootstrapRegistersQaGithubSyncAndHelpRendersCleanly() {
        ICLICommandRegistry registry = new CLICommandRegistry();
        new QaCLIBootstrap().registerCommands(registry);

        assertTrue(registry.findCommand(CLICommandPath.parse("qa github sync")).isPresent(),
                "QaCLIBootstrap must register qa github sync");
        assertTrue(DependencyMap.getDependencyMap().isInstanceRegistered(IGithubIssueReader.class),
                "the typed GitHub issue reader must be DI-registered before commands run");

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        int exitCode = QaCliMain.run(new String[] {"qa", "github", "sync", "--help"},
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

        assertEquals(0, exitCode);
        String text = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Usage: tavall qa github sync"), text);
        for (String option : List.of("--repos", "--owner", "--state", "--dry-run", "--limit", "--project",
                "--section", "--credential")) {
            assertTrue(text.contains(option), "help output is missing " + option + ": " + text);
        }
        assertEquals(0, errBuffer.size(), () -> "help must not write to stderr: " + errBuffer);
        assertTrue(reader.queries.isEmpty(), "rendering help must not read GitHub");
        assertTrue(api.upserts.isEmpty(), "rendering help must not call the QA API");
    }

    private static QaGithubSyncCommand command() {
        return new QaGithubSyncCommand();
    }

    private static QaGithubIssue issue(
            String repository,
            int number,
            String title,
            String body,
            String state,
            String stateReason,
            List<String> labels) {
        return new QaGithubIssue(OWNER, repository, number, title, body, state, stateReason, labels,
                "https://github.com/" + OWNER + "/" + repository + "/issues/" + number);
    }

    private static CLICommandInput input(Map<String, Object> options) {
        return new CLICommandInput(CLICommandPath.of("qa", "github", "sync"),
                List.of("qa", "github", "sync"), Map.of(), options, "tester", Map.of());
    }

    /** Deterministic GitHub reader stub: no GitHub CLI, no network. */
    private static final class StubGithubIssueReader implements IGithubIssueReader {
        final Map<String, List<QaGithubIssue>> issuesByRepository = new LinkedHashMap<>();
        final List<QaGithubIssueQuery> queries = new ArrayList<>();
        QaGithubReaderException failure;

        @Override
        public List<QaGithubIssue> readIssues(QaGithubIssueQuery query) {
            queries.add(query);
            if (failure != null) {
                throw failure;
            }
            return List.copyOf(issuesByRepository.getOrDefault(query.repository(), List.of()));
        }
    }

    /** Deterministic QA execution client stub: no QA API, no credential file, no network. */
    private static final class RecordingQaExecutionApiClient implements IQaExecutionApiClient {
        @Override
        public org.tavall.qa.cli.client.QaCliDesignAiReviewResult recordDesignAiReview(
                org.tavall.qa.cli.client.QaCliDesignAiReviewRequest request) {
            throw new UnsupportedOperationException();
        }

        final List<QaCliTaskUpsertRequest> upserts = new ArrayList<>();
        final Set<String> existingKeys = new LinkedHashSet<>();
        final Set<String> failingKeys = new LinkedHashSet<>();

        @Override
        public QaCliTaskUpsertResult upsertTask(QaCliTaskUpsertRequest request) {
            upserts.add(request);
            if (failingKeys.contains(request.externalKey())) {
                throw new QaCliApiClientException("upsert failed: HTTP 500 for " + request.externalKey(), 500);
            }
            boolean created = !existingKeys.contains(request.externalKey());
            return new QaCliTaskUpsertResult(
                    UUID.nameUUIDFromBytes(request.externalKey().getBytes(StandardCharsets.UTF_8)),
                    request.status(), created, !created, request.externalKey());
        }

        @Override
        public Optional<QaCliDesignReviewSnapshot> getDesignReview(UUID taskId, Path credentialFile) {
            throw new UnsupportedOperationException("qa github sync must not read design reviews");
        }

        @Override
        public QaCliDesignRegisterResult registerDesignCandidates(QaCliDesignRegisterRequest request) {
            throw new UnsupportedOperationException("qa github sync must not register design candidates");
        }
    }

    @Test
    void resultRecordExposesBatchInvariants() {
        QaGithubSyncResult result = new QaGithubSyncResult(OWNER, List.of("tavall-ai"), "open", false,
                2, 1, 1, 0, 0, List.of(TAVALL_AI_197, TAVALL_AI_198), List.of());

        assertTrue(result.successful());
        assertEquals(2, result.synced());
        assertThrows(IllegalArgumentException.class, () -> new QaGithubSyncResult(OWNER, List.of("tavall-ai"),
                "open", false, -1, 0, 0, 0, 0, List.of(), List.of()));
    }
}
