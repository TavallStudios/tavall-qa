package org.tavall.qa.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical versioned payload representation shared by persistence and authorized continuation clients. */
public final class TaskBoardExecutionCheckpointPayloadMapper {
    private static final JsonMapper JSON = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
    private static final int MAXIMUM_BYTES = 1_048_576;

    private TaskBoardExecutionCheckpointPayloadMapper() { }

    public static String toJson(TaskBoardExecutionCheckpointPayloadData value) {
        if (value == null) throw new IllegalArgumentException("Checkpoint payload is required");
        try {
            String encoded = JSON.writeValueAsString(value);
            if (encoded.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_BYTES) {
                throw new IllegalArgumentException("Checkpoint payload exceeds its encoded bound");
            }
            return encoded;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Checkpoint payload cannot be encoded", exception);
        }
    }

    public static TaskBoardExecutionCheckpointPayloadData fromJson(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_BYTES) {
            throw new IllegalArgumentException("Checkpoint payload is missing or exceeds its encoded bound");
        }
        try {
            return JSON.readValue(value, TaskBoardExecutionCheckpointPayloadData.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Checkpoint payload does not match its typed schema", exception);
        }
    }

    public static String sha256(TaskBoardExecutionCheckpointPayloadData value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(toJson(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
