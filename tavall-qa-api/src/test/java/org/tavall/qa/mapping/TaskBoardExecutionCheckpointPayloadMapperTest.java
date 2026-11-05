package org.tavall.qa.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.domain.execution.TaskBoardExecutionDiffData;

final class TaskBoardExecutionCheckpointPayloadMapperTest {
    @Test
    void roundTripsTheTypedPayloadWithAStableDigest() {
        TaskBoardExecutionCheckpointPayloadData payload = payload("Resume the assigned work", List.of("Tests pass"));

        String json = TaskBoardExecutionCheckpointPayloadMapper.toJson(payload);
        TaskBoardExecutionCheckpointPayloadData restored = TaskBoardExecutionCheckpointPayloadMapper.fromJson(json);

        assertEquals(payload, restored);
        assertEquals(json, TaskBoardExecutionCheckpointPayloadMapper.toJson(restored));
        assertEquals(64, TaskBoardExecutionCheckpointPayloadMapper.sha256(payload).length());
        assertEquals(TaskBoardExecutionCheckpointPayloadMapper.sha256(payload),
                TaskBoardExecutionCheckpointPayloadMapper.sha256(restored));
        assertNotEquals(TaskBoardExecutionCheckpointPayloadMapper.sha256(payload),
                TaskBoardExecutionCheckpointPayloadMapper.sha256(
                        payload("Resume after validation", List.of("Tests pass"))));
    }

    @Test
    void rejectsMissingOrOversizedJsonInput() {
        assertThrows(IllegalArgumentException.class, () -> TaskBoardExecutionCheckpointPayloadMapper.fromJson(null));
        assertThrows(IllegalArgumentException.class,
                () -> TaskBoardExecutionCheckpointPayloadMapper.fromJson(" ".repeat(1_048_577)));
    }

    @Test
    void rejectsJsonThatDoesNotMatchTheTypedPayload() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TaskBoardExecutionCheckpointPayloadMapper.fromJson("{not-json"));

        assertTrue(exception.getMessage().contains("typed schema"));
    }

    @Test
    void rejectsAnEncodedPayloadAboveItsByteLimit() {
        List<String> oversizedAcceptance = Collections.nCopies(256, "x".repeat(4096));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TaskBoardExecutionCheckpointPayloadMapper.toJson(payload("Resume", oversizedAcceptance)));

        assertTrue(exception.getMessage().contains("encoded bound"));
    }

    private static TaskBoardExecutionCheckpointPayloadData payload(String instructions,
            List<String> acceptanceConditions) {
        return new TaskBoardExecutionCheckpointPayloadData(
                1,
                instructions,
                acceptanceConditions,
                TaskBoardExecutionDiffData.inline("diff --git a/file b/file\n+change\n"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("tavall-agent-architecture-validation"),
                List.of(),
                List.of(),
                List.of(),
                List.of("tavall-qa:read", "tavall-qa:write"),
                List.of(),
                "Resume after the next verification"
        );
    }
}
