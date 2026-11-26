package org.tavall.qa.cli.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the QA agent credential from the explicit path, environment, or the default agent file. */
public final class QaCliCredentialReader {
    public static final String CREDENTIAL_ENVIRONMENT = "TAVALL_ORCHESTRATOR_AGENT_CREDENTIAL_FILE";
    public static final String DEFAULT_CREDENTIAL_FILE =
            Path.of(System.getProperty("user.home"), ".config", "tavall", "agents",
                    "tavall-orchestrator-agent.json").toString();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QaCliCredentialReader() {
    }

    /**
     * Resolves the effective credential path: explicit option first, then
     * {@code TAVALL_ORCHESTRATOR_AGENT_CREDENTIAL_FILE}, then the default agent credential file.
     */
    public static Path resolve(Path explicitCredentialFile) {
        if (explicitCredentialFile != null && !explicitCredentialFile.toString().isBlank()) {
            return explicitCredentialFile;
        }
        String fromEnvironment = System.getenv(CREDENTIAL_ENVIRONMENT);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return Path.of(fromEnvironment.trim());
        }
        return Path.of(DEFAULT_CREDENTIAL_FILE);
    }

    /** Loads the agent credential JSON ({@code clientId}/{@code clientSecret}); secrets never leave the record. */
    public static QaCliAgentCredential load(Path explicitCredentialFile) {
        Path path = resolve(explicitCredentialFile);
        if (!Files.isRegularFile(path)) {
            throw new QaCliApiClientException("QA agent credential file not found: " + path, 0);
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(Files.readString(path));
        } catch (IOException | UncheckedIOException readEx) {
            throw new QaCliApiClientException("QA agent credential file is unreadable: " + path, 0, readEx);
        }
        String clientId = root.path("clientId").asText(null);
        String clientSecret = root.path("clientSecret").asText(null);
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new QaCliApiClientException(
                    "QA agent credential file is missing clientId or clientSecret: " + path, 0);
        }
        return new QaCliAgentCredential(clientId.trim(), clientSecret, path);
    }
}
