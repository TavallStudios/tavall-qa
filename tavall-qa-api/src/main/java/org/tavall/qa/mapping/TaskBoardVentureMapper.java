package org.tavall.qa.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** Pure JSON mapping for persisted venture string lists. */
public final class TaskBoardVentureMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private TaskBoardVentureMapper() {
    }

    public static List<String> readStringList(String value) {
        try {
            return List.copyOf(MAPPER.readValue(value, STRING_LIST));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Venture list JSON is invalid", exception);
        }
    }
}
