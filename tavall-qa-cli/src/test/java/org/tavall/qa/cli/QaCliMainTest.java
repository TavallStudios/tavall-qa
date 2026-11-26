package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignReviewSnapshot;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dispatch tests of {@link QaCliMain} through the unified Tavall CLI application. */
final class QaCliMainTest {
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final String EXTERNAL_KEY = "github/TavallStudios/tavall-qa/issues/197";

    @TempDir
    Path tempDir;

    private final StubQaExecutionApiClient stub = new StubQaExecutionApiClient();
    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

    @BeforeEach
    void resetDependencies() {
        DependencyMap.getDependencyMap().clear();
        // Registered before boot so QaCLIBootstrap keeps the typed stub instead of the HTTP client.
        DependencyMap.getDependencyMap().registerInstance(IQaExecutionApiClient.class, stub);
    }

    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void dispatchesQaTaskUpsertThroughUnifiedCliApplication() {
        stub.nextUpsertResult = new QaCliTaskUpsertResult(TASK_ID, TaskBoardTaskStatus.ACTIVE, true, false, EXTERNAL_KEY);

        int exitCode = QaCliMain.run(new String[] {
                "qa", "task", "upsert",
                "--external-key", EXTERNAL_KEY,
                "--title", "Package tavall-qa-cli as a runnable artifact",
                "--status", "ACTIVE",
                "--source-reference", "https://github.com/TavallStudios/tavall-ai/issues/197"},
                out, err);

        assertEquals(0, exitCode);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains(TASK_ID.toString()));
        assertEquals(EXTERNAL_KEY, stub.lastUpsertRequest.externalKey());
        assertEquals(TaskBoardTaskStatus.ACTIVE, stub.lastUpsertRequest.status());
    }

    @Test
    void dispatchesQaDesignRegisterThroughUnifiedCliApplication() throws Exception {
        stub.nextRegisterResult = StubQaExecutionApiClient.registerResult(TASK_ID, 3, List.of("A", "B"));
        Path candidates = tempDir.resolve("candidates.json");
        java.nio.file.Files.writeString(candidates, """
                [
                  {"key": "A", "name": "Card list", "previewUrl": "https://qa.tavall.org/a", "rationale": "Dense list"},
                  {"key": "B", "name": "Kanban", "previewUrl": "https://qa.tavall.org/b", "rationale": "Columns"}
                ]
                """);

        int exitCode = QaCliMain.run(new String[] {
                "qa", "design", "register",
                "--task-id", TASK_ID.toString(),
                "--surface", "QA task board",
                "--candidates-json", candidates.toString()},
                out, err);

        assertEquals(0, exitCode);
        assertEquals(TASK_ID, stub.lastRegisterRequest.taskId());
        assertEquals("QA task board", stub.lastRegisterRequest.surface());
        assertEquals(2, stub.lastRegisterRequest.candidates().size());
    }

    @Test
    void dispatchesQaDesignDecisionThroughUnifiedCliApplication() {
        stub.nextDesignReview = Optional.of(new QaCliDesignReviewSnapshot(
                TASK_ID,
                "QA task board",
                3,
                List.of(new TaskBoardDesignCandidate("A", "Card list", "https://qa.tavall.org/a", "Dense list", java.util.Map.of())),
                "A",
                "account-1",
                "2026-11-26T10:00:00Z",
                "Card list wins"));

        int exitCode = QaCliMain.run(new String[] {
                "qa", "design", "decision",
                "--task-id", TASK_ID.toString()},
                out, err);

        assertEquals(0, exitCode);
        assertEquals(TASK_ID, stub.lastDesignReviewTaskId);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("winnerKey"));
    }

    @Test
    void routesQaTaskUpsertHelpToStdoutWithZeroExitCode() {
        int exitCode = QaCliMain.run(new String[] {"qa", "task", "upsert", "--help"}, out, err);

        assertEquals(0, exitCode);
        String text = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Usage: tavall qa task upsert"), text);
        assertTrue(text.contains("--external-key"), text);
        assertTrue(errBuffer.size() == 0, () -> "help must not write to stderr: " + errBuffer);
    }

    @Test
    void routesRootHelpToStdoutWithZeroExitCode() {
        int exitCode = QaCliMain.run(new String[] {"--help"}, out, err);

        assertEquals(0, exitCode);
        String text = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("qa"), text);
    }

    @Test
    void unknownCommandFailsWithExitCodeTwoOnStderr() {
        int exitCode = QaCliMain.run(new String[] {"qa", "bogus"}, out, err);

        assertEquals(2, exitCode);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("UNKNOWN_COMMAND"));
    }

    @Test
    void missingRequiredOptionsFailWithExitCodeTwoOnStderr() {
        int exitCode = QaCliMain.run(new String[] {"qa", "task", "upsert"}, out, err);

        assertEquals(2, exitCode);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("INVALID_ARGUMENT"));
    }

    @Test
    void qaApiClientFailureFailsWithExitCodeOneOnStderr() {
        stub.nextFailure = new QaCliApiClientException("board unreachable", 503);

        int exitCode = QaCliMain.run(new String[] {
                "qa", "design", "decision",
                "--task-id", TASK_ID.toString()},
                out, err);

        assertEquals(1, exitCode);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("QA_API_ERROR"));
    }
}
