package org.tavall.qa.web.pages.project;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Request-specific CSRF and organization context for project-page controls. */
public record QaProjectPageContext(
        String csrfParameterName,
        String csrfToken,
        Optional<UUID> organizationId
) {
    public QaProjectPageContext {
        csrfParameterName = csrfParameterName == null || csrfParameterName.isBlank() ? "_csrf" : csrfParameterName;
        csrfToken = csrfToken == null ? "" : csrfToken;
        Objects.requireNonNull(organizationId, "organizationId");
    }

    public static QaProjectPageContext readOnly() {
        return new QaProjectPageContext("_csrf", "", Optional.empty());
    }
}
