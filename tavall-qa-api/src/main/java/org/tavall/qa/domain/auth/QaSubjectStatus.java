package org.tavall.qa.domain.auth;

/** Request-time projection of Tavall Account standing for QA authorization. */
public enum QaSubjectStatus {
    ANONYMOUS,
    ACTIVE,
    DISABLED,
    MERGED,
    UNAVAILABLE
}
