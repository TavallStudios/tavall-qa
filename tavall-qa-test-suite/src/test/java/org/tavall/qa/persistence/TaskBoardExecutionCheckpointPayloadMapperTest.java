package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.domain.execution.TaskBoardExecutionDiffData;
import org.tavall.qa.mapping.TaskBoardExecutionCheckpointPayloadMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskBoardExecutionCheckpointPayloadMapperTest {
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
}
