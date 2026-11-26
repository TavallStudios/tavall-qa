package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignRegisterRequest;
import org.tavall.qa.cli.client.QaCliDesignRegisterResult;
import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaDesignRegisterCommandTest {
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");

    private final StubQaExecutionApiClient stub = new StubQaExecutionApiClient();

    @TempDir
    Path tempDir;

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
    void registerParsesCandidatesAndDefaultsRevisionToCurrent() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "a", "name": "Board A", "previewUrl": "https://qa.tavall.org/a", "rationale": "fastest to ship"},
                  {"key": "b", "name": "Board B", "previewUrl": "https://qa.tavall.org/b", "rationale": "richest detail",
                   "optionParameters": {"theme": ["dark", "light"]}}
                ]
                """);
        stub.nextRegisterResult = new QaCliDesignRegisterResult(TASK_ID, 4, List.of("A", "B"), null, false);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertTrue(result.successful());
        assertEquals(0, result.exitCode());
        assertEquals(TASK_ID.toString(), result.data().get("taskId"));
        assertEquals("4", result.data().get("revision"));
        assertEquals("A,B", result.data().get("candidates"));

        QaCliDesignRegisterRequest request = stub.lastRegisterRequest;
        assertNotNull(request);
        assertEquals(TASK_ID, request.taskId());
        assertEquals("QA task board", request.surface());
        assertNull(request.expectedRevision(), "revision defaults to the current review at the client boundary");
        assertEquals(2, request.candidates().size());
        TaskBoardDesignCandidate first = request.candidates().get(0);
        assertEquals("A", first.key());
        assertEquals("Board A", first.name());
        assertEquals("https://qa.tavall.org/a", first.previewUrl());
        TaskBoardDesignCandidate second = request.candidates().get(1);
        assertEquals("B", second.key());
        assertEquals(List.of("dark", "light"), second.optionParameters().get("theme"));
    }

    @Test
    void explicitExpectedRevisionIsPassedThrough() throws Exception {
        Path candidatesFile = writeCandidatesJson(twoCandidatesJson());
        stub.nextRegisterResult = StubQaExecutionApiClient.registerResult(TASK_ID, 7, List.of("A", "B"));

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString(),
                "expected-revision", 7)));

        assertTrue(result.successful());
        assertNotNull(stub.lastRegisterRequest);
        assertEquals(7, stub.lastRegisterRequest.expectedRevision());
    }

    @Test
    void credentialOptionIsResolvedIntoRequest() throws Exception {
        Path candidatesFile = writeCandidatesJson(twoCandidatesJson());
        stub.nextRegisterResult = StubQaExecutionApiClient.registerResult(TASK_ID, 1, List.of("A", "B"));

        new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString(),
                "credential", "/tmp/agent-credential.json")));

        assertNotNull(stub.lastRegisterRequest.credentialFile());
        assertEquals("/tmp/agent-credential.json", stub.lastRegisterRequest.credentialFile().toString());
    }

    @Test
    void fewerThanTwoCandidatesIsRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson(
                "[{\"key\": \"A\", \"name\": \"Only\", \"previewUrl\": \"https://qa.tavall.org/a\", \"rationale\": \"x\"}]");

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals(2, result.exitCode());
        assertEquals("candidates JSON must be an array of 2–4 named candidates", result.message());
    }

    @Test
    void moreThanFourCandidatesIsRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "A", "name": "A", "previewUrl": "https://qa.tavall.org/a", "rationale": "x"},
                  {"key": "B", "name": "B", "previewUrl": "https://qa.tavall.org/b", "rationale": "x"},
                  {"key": "C", "name": "C", "previewUrl": "https://qa.tavall.org/c", "rationale": "x"},
                  {"key": "D", "name": "D", "previewUrl": "https://qa.tavall.org/d", "rationale": "x"},
                  {"key": "E", "name": "E", "previewUrl": "https://qa.tavall.org/e", "rationale": "x"}
                ]
                """);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals("candidates JSON must be an array of 2–4 named candidates", result.message());
    }

    @Test
    void unexpectedCandidateFieldsAreRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "A", "name": "A", "previewUrl": "https://qa.tavall.org/a", "rationale": "x", "winner": true},
                  {"key": "B", "name": "B", "previewUrl": "https://qa.tavall.org/b", "rationale": "x"}
                ]
                """);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals("each candidate requires key, name, previewUrl and rationale; optionParameters is optional",
                result.message());
    }

    @Test
    void duplicateCandidateKeysAreRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "A", "name": "A", "previewUrl": "https://qa.tavall.org/a", "rationale": "x"},
                  {"key": "A", "name": "A again", "previewUrl": "https://qa.tavall.org/b", "rationale": "x"}
                ]
                """);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals("candidate keys must be unique A/B/C/D values", result.message());
    }

    @Test
    void nonHttpsPreviewUrlIsRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "A", "name": "A", "previewUrl": "http://qa.tavall.org/a", "rationale": "x"},
                  {"key": "B", "name": "B", "previewUrl": "https://qa.tavall.org/b", "rationale": "x"}
                ]
                """);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals("candidate preview URLs must use HTTPS without credentials", result.message());
    }

    @Test
    void previewUrlWithEmbeddedCredentialsIsRejected() throws Exception {
        Path candidatesFile = writeCandidatesJson("""
                [
                  {"key": "A", "name": "A", "previewUrl": "https://agent:secret@qa.tavall.org/a", "rationale": "x"},
                  {"key": "B", "name": "B", "previewUrl": "https://qa.tavall.org/b", "rationale": "x"}
                ]
                """);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals("candidate preview URLs must use HTTPS without credentials", result.message());
    }

    @Test
    void apiFailureMapsToNonZeroExit() throws Exception {
        Path candidatesFile = writeCandidatesJson(twoCandidatesJson());
        stub.nextFailure = new QaCliApiClientException("candidate registration failed: HTTP 409: conflict", 409);

        CLICommandResult result = new QaDesignRegisterCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "surface", "QA task board",
                "candidates-json", candidatesFile.toString())));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals("QA_API_ERROR", result.failureReason());
        assertEquals("candidate registration failed: HTTP 409: conflict", result.message());
    }

    private static String twoCandidatesJson() {
        return """
                [
                  {"key": "A", "name": "Board A", "previewUrl": "https://qa.tavall.org/a", "rationale": "fastest to ship"},
                  {"key": "B", "name": "Board B", "previewUrl": "https://qa.tavall.org/b", "rationale": "richest detail"}
                ]
                """;
    }

    private Path writeCandidatesJson(String json) throws Exception {
        Path file = tempDir.resolve("candidates-" + System.nanoTime() + ".json");
        Files.writeString(file, json);
        return file;
    }

    private static CLICommandInput input(Map<String, Object> options) {
        return new CLICommandInput(CLICommandPath.of("qa", "design", "register"),
                List.of("qa", "design", "register"), Map.of(), options, "tester", Map.of());
    }
}
