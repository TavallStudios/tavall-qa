package org.tavall.qa.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class TaskBoardDesignCandidateTest {
    @Test
    void omittedOptionParametersNormalizeToAnEmptyMap() {
        TaskBoardDesignCandidate candidate = new TaskBoardDesignCandidate(
                "B", "Console", "https://example.com/preview", "Operator", null);

        assertEquals(Map.of(), candidate.optionParameters());
    }

    @Test
    void optionParametersRemainImmutableAfterNormalization() {
        TaskBoardDesignCandidate candidate = new TaskBoardDesignCandidate(
                "B", "Console", "https://example.com/preview", "Operator");

        assertThrows(UnsupportedOperationException.class,
                () -> candidate.optionParameters().put("density", java.util.List.of("compact")));
    }
}
