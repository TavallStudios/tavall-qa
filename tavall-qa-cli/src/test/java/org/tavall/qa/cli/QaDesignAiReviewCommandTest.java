package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliDesignAiReviewResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaDesignAiReviewCommandTest {
    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void recordsReviewWithDefaultCredentialAndStructuredProvisionalResult() {
        UUID task = UUID.randomUUID();
        var client = new StubQaExecutionApiClient();
        client.nextAiReviewResult = new QaCliDesignAiReviewResult(task, 3, "claude-opus-5-5", "A", false, true);
        DependencyMap.getDependencyMap().clear();
        DependencyMap.getDependencyMap().registerInstance(IQaExecutionApiClient.class, client);

        var result = new QaDesignAiReviewCommand().execute(new CLICommandInput(
                CLICommandPath.of("qa", "design", "ai-review"), List.of("qa", "design", "ai-review"), Map.of(),
                Map.of("task-id", task, "candidate", "A", "rationale", "Hierarchy",
                        "reviewer-model", "claude-opus-5-5", "reviewer-agent-identity", "design-agent",
                        "evidence-refs", "evidence/1, evidence/2"), "tester", Map.of()));

        assertTrue(result.successful(), result.message());
        assertEquals("false", result.data().get("decided"));
        assertEquals("true", result.data().get("deployable"));
        assertEquals(List.of("evidence/1", "evidence/2"), client.lastAiReviewRequest.evidenceRefs());
        assertNull(client.lastAiReviewRequest.expectedRevision());
        assertEquals(List.of(), client.lastAiReviewRequest.artifactRefs());
        assertNull(client.lastAiReviewRequest.credentialFile());
    }
}
