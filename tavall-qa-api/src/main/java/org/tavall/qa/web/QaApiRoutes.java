package org.tavall.qa.web;

/** Canonical read-only API paths owned by the QA API contract. */
public final class QaApiRoutes {
    public static final String VENTURES = "/api/ventures";
    public static final String VENTURE_TASKS = VENTURES + "/{slug}/tasks";

    private QaApiRoutes() {
    }
}
