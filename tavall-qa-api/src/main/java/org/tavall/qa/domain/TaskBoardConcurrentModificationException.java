package org.tavall.qa.domain;

public final class TaskBoardConcurrentModificationException extends RuntimeException {
    public TaskBoardConcurrentModificationException(String message) {
        super(message);
    }
}
