package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.command.option.CLICommandOption;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliTaskUpsertRequest;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaTaskUpsertCommandTest {
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final String EXTERNAL_KEY = "github/TavallStudios/tavall-content/issues/76";

    private final StubQaExecutionApiClient stub = new StubQaExecutionApiClient();

    @BeforeEach
    void registerStub() {
        DependencyMap.getDependencyMap().clear();
        DependencyMap.getDependencyMap().registerInstance(IQaExecutionApiClient.class, stub);
    }

    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void upsertBuildsTypedRequestAndAppliesDefaults() {
        stub.nextUpsertResult = StubQaExecutionApiClient.upsertResult(TASK_ID, EXTERNAL_KEY);

        CLICommandResult result = new QaTaskUpsertCommand().execute(input(Map.of(
                "external-key", EXTERNAL_KEY,
                "title", "Replace qa_task_board.py",
                "status", TaskBoardTaskStatus.ACTIVE)));

        assertTrue(result.successful());
        assertEquals(0, result.exitCode());
        assertEquals(TASK_ID.toString(), result.data().get("taskId"));
        assertEquals("ACTIVE", result.data().get("status"));
        assertEquals("true", result.data().get("created"));
        assertEquals("false", result.data().get("updated"));
        assertEquals(EXTERNAL_KEY, result.data().get("externalKey"));

        QaCliTaskUpsertRequest request = stub.lastUpsertRequest;
        assertNotNull(request);
        assertEquals(EXTERNAL_KEY, request.externalKey());
        assertEquals("Replace qa_task_board.py", request.title());
        assertEquals(TaskBoardTaskStatus.ACTIVE, request.status());
        assertNull(request.description(), "description defaults to the title at the client boundary");
        assertEquals(TaskBoardTaskPriority.NORMAL, request.priority());
        assertEquals("orchestration", request.section());
        assertEquals(QaTaskUpsertCommand.DEFAULT_PROJECT, request.project());
        assertEquals("github-issue", request.sourceType());
        assertNull(request.sourceReference(), "source reference defaults to the external key at the client boundary");
    }

    @Test
    void upsertPassesExplicitOptionsThrough() {
        stub.nextUpsertResult = StubQaExecutionApiClient.upsertResult(TASK_ID, EXTERNAL_KEY);

        CLICommandResult result = new QaTaskUpsertCommand().execute(input(Map.of(
                "external-key", EXTERNAL_KEY,
                "title", "Replace qa_task_board.py",
                "status", TaskBoardTaskStatus.REVIEW,
                "description", "Typed Java CLI parity",
                "priority", TaskBoardTaskPriority.CRITICAL,
                "section", "content",
                "project", "tavall-content",
                "source-type", "manual",
                "source-reference", "https://github.com/TavallStudios/tavall-content/issues/76",
                "credential", "/tmp/agent-credential.json")));

        assertTrue(result.successful());
        QaCliTaskUpsertRequest request = stub.lastUpsertRequest;
        assertNotNull(request);
        assertEquals("Typed Java CLI parity", request.description());
        assertEquals(TaskBoardTaskPriority.CRITICAL, request.priority());
        assertEquals("content", request.section());
        assertEquals("tavall-content", request.project());
        assertEquals("manual", request.sourceType());
        assertEquals("https://github.com/TavallStudios/tavall-content/issues/76", request.sourceReference());
        assertNotNull(request.credentialFile());
        assertEquals("/tmp/agent-credential.json", request.credentialFile().toString());
    }

    @Test
    void missingRequiredOptionsFailCleanly() {
        CLICommandResult result = new QaTaskUpsertCommand().execute(input(Map.of("title", "No status")));

        assertFalse(result.successful());
        assertEquals(2, result.exitCode());
        assertEquals("INVALID_ARGUMENT", result.failureReason());
        assertTrue(result.message().contains("external-key") || result.message().contains("status"),
                "unexpected message: " + result.message());
    }

    @Test
    void invalidStatusValueIsRejectedByTypedConversion() {
        CLICommandOption<?> statusOption = option("status");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> statusOption.convert("WATERLOGGED"));
        assertTrue(failure.getMessage().contains("Unknown enum value"));
    }

    @Test
    void invalidPriorityValueIsRejectedByTypedConversion() {
        CLICommandOption<?> priorityOption = option("priority");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> priorityOption.convert("URGENT"));
        assertTrue(failure.getMessage().contains("Unknown enum value"));
    }

    @Test
    void apiFailureMapsToNonZeroExitWithoutSecrets() {
        stub.nextFailure = new QaCliApiClientException("upsert failed: HTTP 500: <redacted>", 500);

        CLICommandResult result = new QaTaskUpsertCommand().execute(input(Map.of(
                "external-key", EXTERNAL_KEY,
                "title", "Replace qa_task_board.py",
                "status", TaskBoardTaskStatus.ACTIVE)));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals("QA_API_ERROR", result.failureReason());
        assertEquals("upsert failed: HTTP 500: <redacted>", result.message());
    }

    private CLICommandOption<?> option(String name) {
        return new QaTaskUpsertCommand().settings().findOption(name).orElseThrow();
    }

    private static CLICommandInput input(Map<String, Object> options) {
        return new CLICommandInput(CLICommandPath.of("qa", "task", "upsert"),
                List.of("qa", "task", "upsert"), Map.of(), options, "tester", Map.of());
    }
}
