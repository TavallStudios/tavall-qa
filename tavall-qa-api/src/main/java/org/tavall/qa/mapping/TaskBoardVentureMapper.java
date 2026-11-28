package org.tavall.qa.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.tavall.qa.domain.TaskBoardVenture;

import java.util.List;

/** JSON mapping for the venture directory and its persisted string lists. */
public final class TaskBoardVentureMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private TaskBoardVentureMapper() {
    }

    public static String writeDirectory(List<TaskBoardVenture> ventures) {
        try {
            return MAPPER.writeValueAsString(ventures);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Venture directory could not be serialized", exception);
        }
    }

    public static List<String> readStringList(String value) {
        try {
            return List.copyOf(MAPPER.readValue(value, STRING_LIST));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Venture list JSON is invalid", exception);
        }
    }
}
