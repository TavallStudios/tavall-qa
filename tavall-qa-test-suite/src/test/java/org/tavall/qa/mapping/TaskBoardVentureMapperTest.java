package org.tavall.qa.mapping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardVentureMapperTest {
    @Test
    void readsJsonRepositoryArraysAsImmutableTypedValues() {
        List<String> repos = TaskBoardVentureMapper.readStringList("[\"tavall-qa\",\"tavall-web-qa\"]");

        assertEquals(List.of("tavall-qa", "tavall-web-qa"), repos);
        assertThrows(UnsupportedOperationException.class, () -> repos.add("other"));
    }

    @Test
    void rejectsInvalidRepositoryJson() {
        assertThrows(IllegalArgumentException.class, () -> TaskBoardVentureMapper.readStringList("not-json"));
    }
}
