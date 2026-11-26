package org.tavall.qa.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.cli.api.command.CLICommand;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.command.builder.CLICommandBuilder;
import org.tavall.cli.api.command.option.CLICommandOption;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignRegisterRequest;
import org.tavall.qa.cli.client.QaCliDesignRegisterResult;
import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Canonical {@code qa design register} CLI adapter for live design candidates. */
@DelegatesTo(IQaDesignRegisterCommand.class)
public final class QaDesignRegisterCommand extends CLICommand<QaCliDesignRegisterResult>
        implements IQaDesignRegisterCommand, DependencyAccess<IQaExecutionApiClient> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String INVALID_CANDIDATES_MESSAGE =
            "candidates JSON must be an array of 2\u20134 named candidates";
    private static final String CANDIDATE_FIELDS_MESSAGE =
            "each candidate requires key, name, previewUrl and rationale; optionParameters is optional";
    private static final String CANDIDATE_KEYS_MESSAGE = "candidate keys must be unique A/B/C/D values";
    private static final String CANDIDATE_URL_MESSAGE =
            "candidate preview URLs must use HTTPS without credentials";

    public QaDesignRegisterCommand() {
        super(CLICommandBuilder.create("qa", "design", "register")
                .description("Register 2\u20134 live design candidates on a QA task for design review.")
                .option(CLICommandOption.required("task-id", "QA task UUID", UUID.class))
                .option(CLICommandOption.required("surface", "Product page or shared web component being reviewed"))
                .option(CLICommandOption.required("candidates-json",
                        "Path to a JSON array of candidates with key, name, previewUrl and rationale"))
                .option(CLICommandOption.optional("expected-revision",
                        "Expected design review revision; defaults to the current revision", Integer.class, null))
                .option(CLICommandOption.optional("credential",
                        "Path to the QA agent credential JSON; defaults to the orchestrator agent file"))
                .build());
    }

    @Override
    protected QaCliDesignRegisterResult handle(CLICommandInput input) throws Exception {
        Optional<String> candidatesJson = input.findOption("candidates-json");
        List<TaskBoardDesignCandidate> candidates = readCandidates(Path.of(candidatesJson.orElseThrow()));
        Optional<Integer> expectedRevision = input.findOption("expected-revision");
        QaCliDesignRegisterRequest request = new QaCliDesignRegisterRequest(
                input.option("task-id"),
                input.option("surface"),
                candidates,
                expectedRevision.orElse(null),
                QaCliCommandSupport.credentialFile(input));
        return getInstance().registerDesignCandidates(request);
    }

    @Override
    protected CLICommandResult mapResult(QaCliDesignRegisterResult result) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("taskId", result.taskId().toString());
        data.put("revision", Integer.toString(result.revision()));
        data.put("candidates", String.join(",", result.candidateKeys()));
        if (result.decided()) {
            data.put("winnerKey", result.winnerKey());
        }
        return CLICommandResult.success("Registered " + result.candidateKeys().size()
                + " design candidates on task " + result.taskId() + " (revision " + result.revision() + ").",
                data, result);
    }

    @Override
    protected CLICommandResult mapException(Exception exception) {
        if (exception instanceof QaCliApiClientException apiFailure) {
            return CLICommandResult.failure(1, "QA_API_ERROR", apiFailure.getMessage());
        }
        return super.mapException(exception);
    }

    private List<TaskBoardDesignCandidate> readCandidates(Path file) {
        JsonNode root;
        try {
            root = MAPPER.readTree(Files.readString(file));
        } catch (JsonProcessingException invalidJson) {
            throw new IllegalArgumentException(INVALID_CANDIDATES_MESSAGE);
        } catch (IOException | UncheckedIOException readEx) {
            throw new IllegalArgumentException(INVALID_CANDIDATES_MESSAGE, readEx);
        }
        if (!root.isArray() || root.size() < 2 || root.size() > 4) {
            throw new IllegalArgumentException(INVALID_CANDIDATES_MESSAGE);
        }
        Set<String> keys = new HashSet<>();
        List<TaskBoardDesignCandidate> candidates = new ArrayList<>();
        for (JsonNode node : root) {
            if (!node.isObject() || !exactCandidateFields(node)) {
                throw new IllegalArgumentException(CANDIDATE_FIELDS_MESSAGE);
            }
            String key = node.path("key").asText().trim().toUpperCase(Locale.ROOT);
            if (!key.matches("[A-D]") || !keys.add(key)) {
                throw new IllegalArgumentException(CANDIDATE_KEYS_MESSAGE);
            }
            requireHttpsPreviewUrl(node.path("previewUrl").asText());
            candidates.add(parseCandidate(key, node));
        }
        return candidates;
    }

    private static boolean exactCandidateFields(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields.equals(Set.of("key", "name", "previewUrl", "rationale"))
                || fields.equals(Set.of("key", "name", "previewUrl", "rationale", "optionParameters"));
    }

    private static void requireHttpsPreviewUrl(String rawUrl) {
        java.net.URI url;
        try {
            url = java.net.URI.create(rawUrl);
        } catch (IllegalArgumentException invalidUrl) {
            throw new IllegalArgumentException(CANDIDATE_URL_MESSAGE);
        }
        if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null || url.getUserInfo() != null) {
            throw new IllegalArgumentException(CANDIDATE_URL_MESSAGE);
        }
    }

    private static TaskBoardDesignCandidate parseCandidate(String key, JsonNode node) {
        Map<String, List<String>> optionParameters = new LinkedHashMap<>();
        JsonNode options = node.path("optionParameters");
        if (options.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = options.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                List<String> values = new ArrayList<>();
                entry.getValue().forEach(value -> values.add(value.asText()));
                optionParameters.put(entry.getKey(), values);
            }
        }
        return new TaskBoardDesignCandidate(key, node.path("name").asText(), node.path("previewUrl").asText(),
                node.path("rationale").asText(), optionParameters);
    }
}
