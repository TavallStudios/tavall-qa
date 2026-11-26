package org.tavall.qa.cli.client;

import java.nio.file.Path;
import java.util.Objects;

/** Agent credential used to exchange for a short-lived QA execution token; never printed. */
public record QaCliAgentCredential(String clientId, String clientSecret, Path source) {

    public QaCliAgentCredential {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(clientSecret, "clientSecret");
    }

    @Override
    public String toString() {
        return "QaCliAgentCredential{clientId=" + QaCliSecretRedactor.REDACTED
                + ", clientSecret=" + QaCliSecretRedactor.REDACTED
                + ", source=" + source + "}";
    }
}
