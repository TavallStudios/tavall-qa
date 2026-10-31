package org.tavall.qa.domain;

public enum TaskBoardQaStatus {
    NHV("Needs human verification"),
    REVIEW_REQUIRED("Review required"),
    HUMAN_VERIFIED("Human verified");

    private final String displayName;

    TaskBoardQaStatus(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable label for planning surfaces such as the Notion "Current Step" field. */
    public String displayName() {
        return displayName;
    }
}
