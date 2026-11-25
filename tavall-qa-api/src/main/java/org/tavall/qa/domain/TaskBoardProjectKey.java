package org.tavall.qa.domain;

import java.util.Objects;

/**
 * Typed identity for a Task Board project.
 *
 * <p>The engineering board is the single project that holds execution records for work matched to
 * venture owner repositories. Naming it here keeps that key out of handler and presentation
 * string literals.</p>
 */
public record TaskBoardProjectKey(String value) {

    public static final TaskBoardProjectKey ENGINEERING = new TaskBoardProjectKey("tavall-engineering");

    public TaskBoardProjectKey {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
