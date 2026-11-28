package org.tavall.qa.cli.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardDesignReview;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP client for the canonical QA execution endpoints, matching the legacy
 * {@code qa_task_board.py} behavior: client_credentials token exchange, typed
 * task upsert, design-review read, and design-candidate registration.
 * Secrets and access tokens are redacted from every failure message.
 */
public final class HttpQaExecutionApiClient implements IQaExecutionApiClient {
    public static final String USER_AGENT = "tavall-qa-cli/1.0";
    public static final String DEFAULT_TOKEN_URL = "https://account.tavall.org/oauth2/mcp/token";
    public static final String DEFAULT_BASE_URL = "https://qa.tavall.org";
    public static final String TOKEN_URL_ENVIRONMENT = "TAVALL_QA_TOKEN_URL";
    public static final String BASE_URL_ENVIRONMENT = "TAVALL_QA_BASE_URL";
    public static final String TOKEN_RESOURCE = "https://qa.tavall.org";
    public static final String TOKEN_SCOPE = "qa.execution";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int ERROR_BODY_LIMIT = 300;

    private final URI tokenUrl;
    private final URI baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpQaExecutionApiClient() {
        this(defaultTokenUrl(), defaultBaseUrl(), HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    HttpQaExecutionApiClient(URI tokenUrl, URI baseUrl, HttpClient httpClient) {
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    static URI defaultTokenUrl() {
        return URI.create(environmentOr(TOKEN_URL_ENVIRONMENT, DEFAULT_TOKEN_URL));
    }

    static URI defaultBaseUrl() {
        return URI.create(environmentOr(BASE_URL_ENVIRONMENT, DEFAULT_BASE_URL));
    }

    private static String environmentOr(String name, String fallback) {
        String fromEnvironment = System.getenv(name);
        return fromEnvironment == null || fromEnvironment.isBlank() ? fallback : fromEnvironment.trim();
    }

    @Override
    public QaCliTaskUpsertResult upsertTask(QaCliTaskUpsertRequest request) {
        Objects.requireNonNull(request, "request");
        QaCliAgentCredential credential = QaCliCredentialReader.load(request.credentialFile());
        String accessToken = accessToken(credential);
        ObjectNode task = objectMapper.createObjectNode();
        task.put("projectKey", request.project());
        task.put("sectionKey", request.section());
        task.put("title", request.title());
        task.putNull("publicTitle");
        task.put("description", description(request));
        task.putNull("publicSummary");
        task.put("priority", request.priority().name());
        task.put("audience", TaskBoardAudience.ADMIN_ONLY.name());
        task.put("weight", 1);
        task.put("hostValidationRequired", false);
        task.put("qaQcEligible", false);
        task.put("sourceType", request.sourceType());
        task.put("sourceReference", sourceReference(request));
        task.put("sortOrder", 0);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("externalKey", request.externalKey());
        body.set("task", task);
        body.put("status", request.status().name());

        HttpResponse<String> response = post(requestBodyUrl("/api/execution/tasks/upsert"),
                accessToken, credential, body.toString());
        if (response.statusCode() != 200) {
            throw apiFailure("upsert failed", response, accessToken, credential);
        }
        return parseUpsertResult(response.body(), request.externalKey());
    }

    @Override
    public Optional<QaCliDesignReviewSnapshot> getDesignReview(UUID taskId, Path credentialFile) {
        Objects.requireNonNull(taskId, "taskId");
        QaCliAgentCredential credential = QaCliCredentialReader.load(credentialFile);
        String accessToken = accessToken(credential);
        return Optional.ofNullable(readDesignReview(taskId, accessToken, credential));
    }

    @Override
    public QaCliDesignRegisterResult registerDesignCandidates(QaCliDesignRegisterRequest request) {
        Objects.requireNonNull(request, "request");
        QaCliAgentCredential credential = QaCliCredentialReader.load(request.credentialFile());
        String accessToken = accessToken(credential);
        int expectedRevision = request.expectedRevision() != null
                ? request.expectedRevision()
                : currentRevision(request.taskId(), accessToken, credential);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("taskId", request.taskId().toString());
        body.put("surface", request.surface());
        ArrayNode candidates = body.putArray("candidates");
        for (TaskBoardDesignCandidate candidate : request.candidates()) {
            ObjectNode node = candidates.addObject();
            node.put("key", candidate.key());
            node.put("name", candidate.name());
            node.put("previewUrl", candidate.previewUrl());
            node.put("rationale", candidate.rationale());
            if (!candidate.optionParameters().isEmpty()) {
                ObjectNode optionParameters = node.putObject("optionParameters");
                candidate.optionParameters().forEach((optionKey, optionValues) -> {
                    ArrayNode values = optionParameters.putArray(optionKey);
                    optionValues.forEach(values::add);
                });
            }
        }
        body.put("expectedRevision", expectedRevision);

        HttpResponse<String> response = post(requestBodyUrl("/api/execution/tasks/" + request.taskId()
                + "/design-review/candidates"), accessToken, credential, body.toString());
        if (response.statusCode() != 200) {
            throw apiFailure("candidate registration failed", response, accessToken, credential);
        }
        return parseRegisterResult(response.body(), request.taskId(), expectedRevision);
    }

    @Override
    public QaCliDesignAiReviewResult recordDesignAiReview(QaCliDesignAiReviewRequest request) {
        Objects.requireNonNull(request, "request");
        QaCliAgentCredential credential = QaCliCredentialReader.load(request.credentialFile());
        String accessToken = accessToken(credential);
        int expectedRevision = request.expectedRevision() != null ? request.expectedRevision()
                : currentRevision(request.taskId(), accessToken, credential);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("taskId", request.taskId().toString());
        body.put("candidateKey", request.candidateKey());
        body.put("rationale", request.rationale());
        body.put("reviewerModel", request.reviewerModel());
        body.put("reviewerAgentIdentity", request.reviewerAgentIdentity());
        body.put("expectedRevision", expectedRevision);
        if (request.screenshotArtifactRef() != null && !request.screenshotArtifactRef().isBlank()) {
            body.put("screenshotArtifactRef", request.screenshotArtifactRef());
        }
        ArrayNode evidenceRefs = body.putArray("evidenceRefs");
        request.evidenceRefs().forEach(evidenceRefs::add);
        ArrayNode artifactRefs = body.putArray("artifactRefs");
        request.artifactRefs().forEach(artifactRefs::add);
        HttpResponse<String> response = post(requestBodyUrl("/api/execution/tasks/" + request.taskId()
                + "/design-review/ai-review"), accessToken, credential, body.toString());
        if (response.statusCode() != 200) {
            throw apiFailure("AI design review failed", response, accessToken, credential);
        }
        return parseAiReviewResult(response.body(), request.taskId());
    }

    private QaCliDesignAiReviewResult parseAiReviewResult(String body, UUID expectedTaskId) {
        try {
            JsonNode root = objectMapper.readTree(body);
            UUID taskId = UUID.fromString(root.required("taskId").asText());
            String model = root.required("aiReviewerModel").asText();
            String candidate = root.required("aiCandidateKey").asText();
            int revision = root.required("revision").asInt();
            if (!expectedTaskId.equals(taskId) || !TaskBoardDesignReview.isApprovedAiReviewerModel(model)
                    || !candidate.matches("[A-D]") || revision < 1) {
                throw new IllegalArgumentException("Invalid AI design review response");
            }
            boolean decided = root.hasNonNull("winnerKey") || root.path("decided").asBoolean();
            return new QaCliDesignAiReviewResult(taskId, revision, model, candidate, decided, true);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new QaCliApiClientException("Could not parse AI design review response", 200);
        }
    }

    private int currentRevision(UUID taskId, String accessToken, QaCliAgentCredential credential) {
        QaCliDesignReviewSnapshot current = readDesignReview(taskId, accessToken, credential);
        return current == null ? 0 : current.revision();
    }

    private QaCliDesignReviewSnapshot readDesignReview(UUID taskId, String accessToken,
                                                       QaCliAgentCredential credential) {
        HttpRequest request = HttpRequest.newBuilder(requestBodyUrl("/api/execution/tasks/" + taskId
                        + "/design-review"))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw apiFailure("design review read failed", response, accessToken, credential);
        }
        try {
            return parseDesignReview(response.body(), taskId);
        } catch (JsonProcessingException parseEx) {
            throw new QaCliApiClientException("design review response was not valid JSON", response.statusCode());
        }
    }

    private QaCliTaskUpsertResult parseUpsertResult(String body, String externalKey) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException parseEx) {
            throw new QaCliApiClientException("upsert response was not valid JSON", 200);
        }
        JsonNode task = root.path("task");
        UUID taskId = task.hasNonNull("taskId") ? UUID.fromString(task.get("taskId").asText()) : null;
        TaskBoardTaskStatus status = task.hasNonNull("status")
                ? TaskBoardTaskStatus.valueOf(task.get("status").asText())
                : null;
        return new QaCliTaskUpsertResult(taskId, status,
                root.path("created").asBoolean(false),
                root.path("updated").asBoolean(false),
                externalKey);
    }

    private QaCliDesignRegisterResult parseRegisterResult(String body, UUID fallbackTaskId, int fallbackRevision) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException parseEx) {
            throw new QaCliApiClientException("candidate registration response was not valid JSON", 200);
        }
        UUID taskId = root.hasNonNull("taskId") ? UUID.fromString(root.get("taskId").asText()) : fallbackTaskId;
        int revision = root.path("revision").asInt(fallbackRevision);
        List<String> candidateKeys = new ArrayList<>();
        for (JsonNode candidate : root.path("candidates")) {
            candidateKeys.add(candidate.path("key").asText());
        }
        String winnerKey = root.hasNonNull("winnerKey") ? root.get("winnerKey").asText() : null;
        return new QaCliDesignRegisterResult(taskId, revision, candidateKeys, winnerKey, winnerKey != null);
    }

    private QaCliDesignReviewSnapshot parseDesignReview(String body, UUID fallbackTaskId)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);
        UUID taskId = root.hasNonNull("taskId") ? UUID.fromString(root.get("taskId").asText()) : fallbackTaskId;
        List<TaskBoardDesignCandidate> candidates = new ArrayList<>();
        for (JsonNode node : root.path("candidates")) {
            candidates.add(parseCandidate(node));
        }
        return new QaCliDesignReviewSnapshot(
                taskId,
                root.hasNonNull("surface") ? root.get("surface").asText() : null,
                root.path("revision").asInt(0),
                candidates,
                root.hasNonNull("winnerKey") ? root.get("winnerKey").asText() : null,
                root.hasNonNull("decidedByAccountId") ? root.get("decidedByAccountId").asText() : null,
                root.hasNonNull("decidedAt") ? root.get("decidedAt").asText() : null,
                root.path("note").asText(""),
                root.hasNonNull("aiReviewerModel") ? root.get("aiReviewerModel").asText() : null,
                root.hasNonNull("aiReviewerAccountId") ? root.get("aiReviewerAccountId").asText() : null,
                root.hasNonNull("aiReviewerAgentIdentity") ? root.get("aiReviewerAgentIdentity").asText() : null,
                root.hasNonNull("aiReviewedAt") ? root.get("aiReviewedAt").asText() : null,
                root.hasNonNull("aiCandidateKey") ? root.get("aiCandidateKey").asText() : null,
                root.hasNonNull("aiRationale") ? root.get("aiRationale").asText() : null,
                readStringList(root.path("aiEvidenceRefs")),
                readStringList(root.path("aiArtifactRefs")),
                root.hasNonNull("aiScreenshotArtifactRef") ? root.get("aiScreenshotArtifactRef").asText() : null);
    }

    private TaskBoardDesignCandidate parseCandidate(JsonNode node) {
        Map<String, List<String>> optionParameters = new LinkedHashMap<>();
        JsonNode options = node.path("optionParameters");
        if (options.isObject()) {
            options.fields().forEachRemaining(entry -> {
                List<String> values = new ArrayList<>();
                entry.getValue().forEach(value -> values.add(value.asText()));
                optionParameters.put(entry.getKey(), values);
            });
        }
        return new TaskBoardDesignCandidate(
                node.path("key").asText(),
                node.path("name").asText(),
                node.path("previewUrl").asText(),
                node.path("rationale").asText(),
                optionParameters);
    }

    private String accessToken(QaCliAgentCredential credential) {
        String basic = Base64.getEncoder().encodeToString((credential.clientId() + ":" + credential.clientSecret())
                .getBytes(StandardCharsets.UTF_8));
        String form = "grant_type=client_credentials"
                + "&resource=" + urlEncode(TOKEN_RESOURCE)
                + "&scope=" + urlEncode(TOKEN_SCOPE);
        HttpRequest request = HttpRequest.newBuilder(tokenUrl)
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new QaCliApiClientException("token request failed: HTTP " + response.statusCode(),
                    response.statusCode());
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (JsonProcessingException parseEx) {
            throw new QaCliApiClientException("token response was not valid JSON", response.statusCode());
        }
        String token = root.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new QaCliApiClientException("token response did not contain an access_token", response.statusCode());
        }
        return token;
    }

    private HttpResponse<String> post(URI url, String accessToken, QaCliAgentCredential credential, String body) {
        HttpRequest request = HttpRequest.newBuilder(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | UncheckedIOException sendEx) {
            throw new QaCliApiClientException(
                    "QA execution API request failed: " + sendEx.getClass().getSimpleName(), 0, sendEx);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new QaCliApiClientException("QA execution API request was interrupted", 0, interrupted);
        }
    }

    private QaCliApiClientException apiFailure(String operation, HttpResponse<String> response,
                                               String accessToken, QaCliAgentCredential credential) {
        String redacted = QaCliSecretRedactor.redact(response.body(), accessToken, credential.clientSecret());
        return new QaCliApiClientException(operation + ": HTTP " + response.statusCode() + ": "
                + truncate(redacted), response.statusCode());
    }

    private URI requestBodyUrl(String path) {
        return URI.create(baseUrl.toString() + path);
    }

    private static String description(QaCliTaskUpsertRequest request) {
        return request.description() == null || request.description().isBlank()
                ? request.title()
                : request.description();
    }

    private static String sourceReference(QaCliTaskUpsertRequest request) {
        return request.sourceReference() == null || request.sourceReference().isBlank()
                ? request.externalKey()
                : request.sourceReference();
    }

    private static List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (value != null && !value.isNull()) {
                values.add(value.asText());
            }
        }
        return values;
    }

    private static String truncate(String text) {
        return text.length() <= ERROR_BODY_LIMIT ? text : text.substring(0, ERROR_BODY_LIMIT);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
