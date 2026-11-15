package org.tavall.qa.web.pages.task;

import java.util.Objects;

/** Typed reference to an external execution evidence artifact (compile, test, or CI). */
public record QaTaskExecutionEvidenceLink(
        String label,
        String url,
        QaExecutionEvidenceType type
) {
    public QaTaskExecutionEvidenceLink {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(type, "type");
    }

    public static QaTaskExecutionEvidenceLink compile(String label, String url) {
        return new QaTaskExecutionEvidenceLink(label, url, QaExecutionEvidenceType.COMPILE);
    }

    public static QaTaskExecutionEvidenceLink test(String label, String url) {
        return new QaTaskExecutionEvidenceLink(label, url, QaExecutionEvidenceType.TEST);
    }

    public static QaTaskExecutionEvidenceLink ci(String label, String url) {
        return new QaTaskExecutionEvidenceLink(label, url, QaExecutionEvidenceType.CI);
    }
}
