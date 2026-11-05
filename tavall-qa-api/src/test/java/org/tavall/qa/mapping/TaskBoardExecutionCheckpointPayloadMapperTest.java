package org.tavall.qa.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.domain.execution.TaskBoardExecutionDiffData;

final class TaskBoardExecutionCheckpointPayloadMapperTest {
    @Test
    void versionOneHasAStableCrossClientRepresentationAndDigest() throws Exception {
        String canonical;
        try (var input = getClass().getClassLoader().getResourceAsStream("checkpoints/payload-v1.json")) {
            assertNotNull(input);
            canonical = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        TaskBoardExecutionCheckpointPayloadData decoded = TaskBoardExecutionCheckpointPayloadMapper.fromJson(canonical);
        assertEquals(canonical, TaskBoardExecutionCheckpointPayloadMapper.toJson(decoded));
        assertEquals("e283262c0376e5fd4f7e8d6ffbcf15170fe405b76748e49165d0d1698b24d672",
                TaskBoardExecutionCheckpointPayloadMapper.sha256(decoded));
        assertEquals("compile production", decoded.nextAction());
        assertThrows(IllegalArgumentException.class,
                () -> TaskBoardExecutionCheckpointPayloadMapper.fromJson(canonical.replace("\"schemaVersion\":1", "\"schemaVersion\":2")));
        assertThrows(IllegalArgumentException.class,
                () -> TaskBoardExecutionCheckpointPayloadMapper.fromJson(canonical.replace("\"instructions\":", "\"unknownField\":true,\"instructions\":")));
    }

    @Test
    void diffBytesMustMatchTheirDigestAndDuplicateCapabilitiesAreCanonicalized() {
        TaskBoardExecutionDiffData diff = TaskBoardExecutionDiffData.inline("diff --git a/a.java b/a.java\n");
        assertThrows(IllegalArgumentException.class, () -> new TaskBoardExecutionDiffData(diff.sha256(), "different bytes", null));
        TaskBoardExecutionCheckpointPayloadData payload = new TaskBoardExecutionCheckpointPayloadData(1, "Task instructions",
                List.of("Compile"), diff, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of("REPOSITORY_WRITE", "REPOSITORY_READ", "REPOSITORY_WRITE"),
                List.of(), "Compile production");
        assertEquals(List.of("REPOSITORY_READ", "REPOSITORY_WRITE"), payload.requiredCapabilities());
    }

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
