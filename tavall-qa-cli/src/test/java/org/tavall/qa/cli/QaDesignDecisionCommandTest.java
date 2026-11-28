package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignReviewSnapshot;
import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaDesignDecisionCommandTest {
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");

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
    void decidedReviewIsReportedWithAttribution() {
        stub.nextDesignReview = java.util.Optional.of(new QaCliDesignReviewSnapshot(
                TASK_ID, "QA task board", 4,
                List.of(new TaskBoardDesignCandidate("A", "Board A", "https://qa.tavall.org/a", "fast"),
                        new TaskBoardDesignCandidate("C", "Board C", "https://qa.tavall.org/c", "rich")),
                "C", ACCOUNT_ID.toString(), "2026-11-26T10:00:00Z", "Attribution chosen for provenance"));

        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of("task-id", TASK_ID)));

        assertTrue(result.successful());
        assertEquals(TASK_ID.toString(), result.data().get("taskId"));
        assertEquals("true", result.data().get("decided"));
        assertEquals("C", result.data().get("winnerKey"));
        assertEquals(ACCOUNT_ID.toString(), result.data().get("decidedByAccountId"));
        assertEquals("2026-11-26T10:00:00Z", result.data().get("decidedAt"));
        assertEquals("Attribution chosen for provenance", result.data().get("note"));
        assertEquals("4", result.data().get("revision"));
        assertEquals("A,C", result.data().get("candidates"));
        assertEquals(TASK_ID, stub.lastDesignReviewTaskId);
        assertNull(stub.lastDesignReviewCredentialFile);
    }

    @Test
    void undecidedReviewReportsDecidedFalse() {
        stub.nextDesignReview = java.util.Optional.of(new QaCliDesignReviewSnapshot(
                TASK_ID, "QA task board", 2,
                List.of(new TaskBoardDesignCandidate("A", "Board A", "https://qa.tavall.org/a", "fast"),
                        new TaskBoardDesignCandidate("B", "Board B", "https://qa.tavall.org/b", "rich")),
                null, null, null, ""));

        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of("task-id", TASK_ID)));

        assertTrue(result.successful());
        assertEquals("false", result.data().get("decided"));
        assertFalse(result.data().containsKey("winnerKey"));
    }

    @Test
    void provisionalReviewReportsDeployableWithNoHumanWinner() {
        stub.nextDesignReview = java.util.Optional.of(new QaCliDesignReviewSnapshot(
                TASK_ID, "QA task board", 2,
                List.of(new TaskBoardDesignCandidate("A", "Board A", "https://qa.tavall.org/a", "fast"),
                        new TaskBoardDesignCandidate("B", "Board B", "https://qa.tavall.org/b", "rich")),
                null, null, null, "", "claude-opus-5-5", ACCOUNT_ID.toString(), "design-agent",
                "2026-11-28T00:00:00Z", "A", "Rationale", List.of("evidence/1"), List.of(), null));

        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of("task-id", TASK_ID)));
        assertTrue(result.successful());
        assertEquals("false", result.data().get("decided"));
        assertEquals("true", result.data().get("aiReviewed"));
        assertEquals("true", result.data().get("deployable"));
        assertEquals("A", result.data().get("aiCandidateKey"));
        assertEquals("claude-opus-5-5", result.data().get("aiReviewerModel"));
        assertFalse(result.data().containsKey("winnerKey"));
    }

    @Test
    void missingReviewFailsWithParityMessage() {
        stub.nextDesignReview = java.util.Optional.empty();

        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of("task-id", TASK_ID)));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals("DESIGN_REVIEW_UNAVAILABLE", result.failureReason());
        assertEquals("design review not found or unavailable for this Account", result.message());
    }

    @Test
    void credentialOptionIsForwardedToTheClient() {
        stub.nextDesignReview = java.util.Optional.of(new QaCliDesignReviewSnapshot(
                TASK_ID, "QA task board", 1,
                List.of(new TaskBoardDesignCandidate("A", "Board A", "https://qa.tavall.org/a", "fast"),
                        new TaskBoardDesignCandidate("B", "Board B", "https://qa.tavall.org/b", "rich")),
                null, null, null, ""));

        new QaDesignDecisionCommand().execute(input(Map.of(
                "task-id", TASK_ID,
                "credential", "/tmp/agent-credential.json")));

        assertNotNull(stub.lastDesignReviewCredentialFile);
        assertEquals("/tmp/agent-credential.json", stub.lastDesignReviewCredentialFile.toString());
    }

    @Test
    void apiFailureMapsToNonZeroExit() {
        stub.nextFailure = new QaCliApiClientException("design review read failed: HTTP 503: <redacted>", 503);

        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of("task-id", TASK_ID)));

        assertFalse(result.successful());
        assertEquals(1, result.exitCode());
        assertEquals("QA_API_ERROR", result.failureReason());
        assertEquals("design review read failed: HTTP 503: <redacted>", result.message());
    }

    @Test
    void missingTaskIdFailsCleanly() {
        CLICommandResult result = new QaDesignDecisionCommand().execute(input(Map.of()));

        assertFalse(result.successful());
        assertEquals(2, result.exitCode());
        assertEquals("INVALID_ARGUMENT", result.failureReason());
    }

    private static CLICommandInput input(Map<String, Object> options) {
        return new CLICommandInput(CLICommandPath.of("qa", "design", "decision"),
                List.of("qa", "design", "decision"), Map.of(), options, "tester", Map.of());
    }
}
