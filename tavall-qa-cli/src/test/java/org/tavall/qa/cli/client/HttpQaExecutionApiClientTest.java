package org.tavall.qa.cli.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HttpQaExecutionApiClientTest {
    private static final String TOKEN = "short-lived-qa-access-token";
    private static final String CLIENT_ID = "qa-agent-client-id";
    private static final String CLIENT_SECRET = "qa-agent-client-secret";
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<RecordedRequest> requests = new ArrayList<>();

    private HttpServer server;
    private HttpQaExecutionApiClient client;
    private Path credentialFile;

    private volatile int tokenStatus = 200;
    private volatile String tokenBody = "{\"access_token\": \"" + TOKEN + "\"}";
    private volatile int upsertStatus = 200;
    private volatile String upsertBody =
            "{\"task\":{\"taskId\":\"" + TASK_ID + "\",\"status\":\"ACTIVE\"},\"created\":true,\"updated\":false}";
    private volatile int reviewStatus = 200;
    private volatile String reviewBody = decidedReviewJson(3);
    private volatile int candidatesStatus = 200;
    private volatile String candidatesBody =
            "{\"taskId\":\"" + TASK_ID + "\",\"revision\":4,\"candidates\":"
                    + "[{\"key\":\"A\",\"name\":\"Board A\",\"previewUrl\":\"https://qa.tavall.org/a\",\"rationale\":\"fast\"},"
                    + "{\"key\":\"B\",\"name\":\"Board B\",\"previewUrl\":\"https://qa.tavall.org/b\",\"rationale\":\"rich\"}]}";

    @BeforeEach
    void start() throws IOException {
        credentialFile = Files.createTempFile("qa-agent-credential", ".json");
        Files.writeString(credentialFile, "{\"clientId\": \"" + CLIENT_ID
                + "\", \"clientSecret\": \"" + CLIENT_SECRET + "\"}");
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/oauth2/mcp/token", this::handleToken);
        server.createContext("/api/execution/tasks/upsert", this::handleUpsert);
        server.createContext("/api/execution/tasks", this::handleTasks);
        server.start();
        int port = server.getAddress().getPort();
        client = new HttpQaExecutionApiClient(
                URI.create("http://localhost:" + port + "/oauth2/mcp/token"),
                URI.create("http://localhost:" + port),
                HttpClient.newHttpClient());
    }

    @AfterEach
    void stop() throws IOException {
        server.stop(0);
        Files.deleteIfExists(credentialFile);
    }

    @Test
    void tokenExchangeUsesBasicAuthAndFormBody() {
        client.upsertTask(upsertRequest(null, null));

        RecordedRequest tokenRequest = request("/oauth2/mcp/token");
        assertEquals("Basic " + Base64.getEncoder().encodeToString(
                        (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8)),
                firstHeader(tokenRequest, "Authorization"));
        assertEquals("application/x-www-form-urlencoded", firstHeader(tokenRequest, "Content-Type"));
        assertEquals(HttpQaExecutionApiClient.USER_AGENT, firstHeader(tokenRequest, "User-Agent"));
        String form = tokenRequest.body();
        assertTrue(form.contains("grant_type=client_credentials"), form);
        assertTrue(form.contains("resource=https%3A%2F%2Fqa.tavall.org"), form);
        assertTrue(form.contains("scope=qa.execution"), form);
    }

    @Test
    void upsertSendsCanonicalPayloadAndParsesResult() throws Exception {
        QaCliTaskUpsertResult result = client.upsertTask(upsertRequest(null, null));

        RecordedRequest upsert = request("/api/execution/tasks/upsert");
        assertEquals("POST", upsert.method());
        assertEquals("Bearer " + TOKEN, firstHeader(upsert, "Authorization"));
        assertEquals(HttpQaExecutionApiClient.USER_AGENT, firstHeader(upsert, "User-Agent"));
        JsonNode body = mapper.readTree(upsert.body());
        assertEquals(EXTERNAL_KEY, body.path("externalKey").asText());
        assertEquals("ACTIVE", body.path("status").asText());
        JsonNode task = body.path("task");
        assertEquals("tavall-engineering", task.path("projectKey").asText());
        assertEquals("orchestration", task.path("sectionKey").asText());
        assertEquals("Replace qa_task_board.py", task.path("title").asText());
        assertEquals("Replace qa_task_board.py", task.path("description").asText());
        assertTrue(task.has("publicTitle") && task.get("publicTitle").isNull());
        assertTrue(task.has("publicSummary") && task.get("publicSummary").isNull());
        assertEquals("NORMAL", task.path("priority").asText());
        assertEquals("ADMIN_ONLY", task.path("audience").asText());
        assertEquals(1, task.path("weight").asInt());
        assertFalse(task.path("hostValidationRequired").asBoolean());
        assertFalse(task.path("qaQcEligible").asBoolean());
        assertEquals("github-issue", task.path("sourceType").asText());
        assertEquals(EXTERNAL_KEY, task.path("sourceReference").asText());
        assertEquals(0, task.path("sortOrder").asInt());

        assertEquals(TASK_ID, result.taskId());
        assertEquals(TaskBoardTaskStatus.ACTIVE, result.status());
        assertTrue(result.created());
        assertFalse(result.updated());
        assertEquals(EXTERNAL_KEY, result.externalKey());
    }

    @Test
    void upsertPassesExplicitDescriptionAndSourceReference() throws Exception {
        client.upsertTask(upsertRequest("Typed parity", "https://github.com/TavallStudios/tavall-qa/pull/1"));

        JsonNode task = mapper.readTree(request("/api/execution/tasks/upsert").body()).path("task");
        assertEquals("Typed parity", task.path("description").asText());
        assertEquals("https://github.com/TavallStudios/tavall-qa/pull/1", task.path("sourceReference").asText());
        assertEquals("NORMAL", task.path("priority").asText());
        assertEquals("orchestration", task.path("sectionKey").asText());
        assertEquals("tavall-engineering", task.path("projectKey").asText());
    }

    @Test
    void upsertFailureRedactsAccessTokenAndSecretFromErrorBody() {
        upsertStatus = 500;
        upsertBody = "upsert exploded: Bearer " + TOKEN + " and secret " + CLIENT_SECRET;

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.upsertTask(upsertRequest(null, null)));

        assertEquals(500, failure.statusCode());
        assertTrue(failure.getMessage().startsWith("upsert failed: HTTP 500: "), failure.getMessage());
        assertFalse(failure.getMessage().contains(TOKEN), failure.getMessage());
        assertFalse(failure.getMessage().contains(CLIENT_SECRET), failure.getMessage());
        assertTrue(failure.getMessage().contains(QaCliSecretRedactor.REDACTED), failure.getMessage());
    }

    @Test
    void upsertFailureMessageIsTruncated() {
        upsertStatus = 500;
        upsertBody = "x".repeat(1000);

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.upsertTask(upsertRequest(null, null)));

        assertTrue(failure.getMessage().length() <= "upsert failed: HTTP 500: ".length() + 300);
    }

    @Test
    void tokenFailureReportsOnlyStatus() {
        tokenStatus = 401;
        tokenBody = "{\"error\": \"" + CLIENT_SECRET + "\"}";

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.upsertTask(upsertRequest(null, null)));

        assertEquals(401, failure.statusCode());
        assertEquals("token request failed: HTTP 401", failure.getMessage());
        assertFalse(failure.getMessage().contains(CLIENT_SECRET));
        assertFalse(failure.getMessage().contains("error"));
    }

    @Test
    void aiReviewPostsProvisionalAttributionAndUsesTheCurrentRevision() throws Exception {
        var result = client.recordDesignAiReview(new QaCliDesignAiReviewRequest(TASK_ID, "A", "Hierarchy",
                List.of("evidence/1"), List.of("artifact/1"), "screenshot/1",
                "gemini-4-neon-medium", "neon-reviewer", null, credentialFile));

        var sent = request("/api/execution/tasks/" + TASK_ID + "/design-review/ai-review");
        assertEquals("POST", sent.method());
        JsonNode body = mapper.readTree(sent.body());
        assertEquals(3, body.path("expectedRevision").asInt());
        assertEquals("gemini-4-neon-medium", body.path("reviewerModel").asText());
        assertEquals("neon-reviewer", body.path("reviewerAgentIdentity").asText());
        assertEquals("evidence/1", body.path("evidenceRefs").get(0).asText());
        assertEquals("artifact/1", body.path("artifactRefs").get(0).asText());
        assertEquals("screenshot/1", body.path("screenshotArtifactRef").asText());
        assertFalse(result.decided());
        assertTrue(result.deployable());
        assertEquals("A", result.aiCandidateKey());
        assertEquals(4, result.revision());
    }

    @Test
    void designReviewParsesDecidedSnapshot() {
        reviewBody = decidedReviewJson(3);

        Optional<QaCliDesignReviewSnapshot> review = client.getDesignReview(TASK_ID, credentialFile);

        assertTrue(review.isPresent());
        QaCliDesignReviewSnapshot snapshot = review.get();
        assertEquals(TASK_ID, snapshot.taskId());
        assertEquals("QA task board", snapshot.surface());
        assertEquals(3, snapshot.revision());
        assertTrue(snapshot.decided());
        assertEquals("C", snapshot.winnerKey());
        assertEquals("00000000-0000-0000-0000-000000000901", snapshot.decidedByAccountId());
        assertEquals("2026-11-26T10:00:00Z", snapshot.decidedAt());
        assertEquals("Attribution chosen for provenance", snapshot.note());
        assertEquals(List.of("A", "B", "C"),
                snapshot.candidates().stream().map(c -> c.key()).toList());

        RecordedRequest reviewRequest = request("/api/execution/tasks/" + TASK_ID + "/design-review");
        assertEquals("GET", reviewRequest.method());
        assertEquals("Bearer " + TOKEN, firstHeader(reviewRequest, "Authorization"));
    }

    @Test
    void designReviewMissingReturnsEmpty() {
        reviewStatus = 404;

        Optional<QaCliDesignReviewSnapshot> review = client.getDesignReview(TASK_ID, credentialFile);

        assertTrue(review.isEmpty());
    }

    @Test
    void designReviewFailureRedactsAccessToken() {
        reviewStatus = 502;
        reviewBody = "review exploded: " + TOKEN;

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.getDesignReview(TASK_ID, credentialFile));

        assertEquals(502, failure.statusCode());
        assertTrue(failure.getMessage().startsWith("design review read failed: HTTP 502: "), failure.getMessage());
        assertFalse(failure.getMessage().contains(TOKEN));
        assertTrue(failure.getMessage().contains(QaCliSecretRedactor.REDACTED));
    }

    @Test
    void registerDefaultsExpectedRevisionFromCurrentReview() throws Exception {
        reviewBody = undecidedReviewJson(3);

        client.registerDesignCandidates(registerRequest(null));

        RecordedRequest reviewRequest = request("/api/execution/tasks/" + TASK_ID + "/design-review");
        assertNotNull(reviewRequest, "current review must be read to default the expected revision");
        JsonNode body = mapper.readTree(request("/api/execution/tasks/" + TASK_ID + "/design-review/candidates").body());
        assertEquals(3, body.path("expectedRevision").asInt());
    }

    @Test
    void registerWithoutCurrentReviewDefaultsToZero() throws Exception {
        reviewStatus = 404;

        client.registerDesignCandidates(registerRequest(null));

        JsonNode body = mapper.readTree(request("/api/execution/tasks/" + TASK_ID + "/design-review/candidates").body());
        assertEquals(0, body.path("expectedRevision").asInt());
    }

    @Test
    void explicitExpectedRevisionSkipsCurrentReviewRead() throws Exception {
        client.registerDesignCandidates(registerRequest(7));

        assertTrue(requests.stream().noneMatch(r -> r.path().endsWith("/design-review")),
                "explicit expected revision must not read the current review");
        JsonNode body = mapper.readTree(request("/api/execution/tasks/" + TASK_ID + "/design-review/candidates").body());
        assertEquals(7, body.path("expectedRevision").asInt());
    }

    @Test
    void registerSendsCanonicalCandidatePayload() throws Exception {
        candidatesBody = "{\"taskId\":\"" + TASK_ID + "\",\"revision\":1,\"candidates\":[]}";
        client.registerDesignCandidates(registerRequest(1));

        RecordedRequest candidatesRequest = request("/api/execution/tasks/" + TASK_ID + "/design-review/candidates");
        assertEquals("POST", candidatesRequest.method());
        assertEquals("Bearer " + TOKEN, firstHeader(candidatesRequest, "Authorization"));
        JsonNode body = mapper.readTree(candidatesRequest.body());
        assertEquals(TASK_ID.toString(), body.path("taskId").asText());
        assertEquals("QA task board", body.path("surface").asText());
        JsonNode candidates = body.path("candidates");
        assertEquals(2, candidates.size());
        JsonNode first = candidates.get(0);
        assertEquals("A", first.path("key").asText());
        assertEquals("Board A", first.path("name").asText());
        assertEquals("https://qa.tavall.org/a", first.path("previewUrl").asText());
        assertEquals("fastest to ship", first.path("rationale").asText());
        JsonNode second = candidates.get(1);
        assertEquals("B", second.path("key").asText());
        assertEquals("dark", second.path("optionParameters").path("theme").get(0).asText());
        assertEquals("light", second.path("optionParameters").path("theme").get(1).asText());
    }

    @Test
    void registerParsesRegistrationOutcome() {
        reviewBody = undecidedReviewJson(3);

        QaCliDesignRegisterResult result = client.registerDesignCandidates(registerRequest(null));

        assertEquals(TASK_ID, result.taskId());
        assertEquals(4, result.revision());
        assertEquals(List.of("A", "B"), result.candidateKeys());
        assertFalse(result.decided());
        assertNull(result.winnerKey());
    }

    @Test
    void candidateRegistrationFailureRedactsAccessToken() {
        reviewStatus = 404;
        candidatesStatus = 409;
        candidatesBody = "conflict: Bearer " + TOKEN + " secret " + CLIENT_SECRET;

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.registerDesignCandidates(registerRequest(2)));

        assertEquals(409, failure.statusCode());
        assertTrue(failure.getMessage().startsWith("candidate registration failed: HTTP 409: "), failure.getMessage());
        assertFalse(failure.getMessage().contains(TOKEN));
        assertFalse(failure.getMessage().contains(CLIENT_SECRET));
        assertTrue(failure.getMessage().contains(QaCliSecretRedactor.REDACTED));
    }

    @Test
    void missingCredentialFailsCleanlyWithoutSecrets() throws Exception {
        Files.delete(credentialFile);

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.upsertTask(upsertRequest(null, null)));

        assertEquals(0, failure.statusCode());
        assertTrue(failure.getMessage().startsWith("QA agent credential file not found: "), failure.getMessage());
        assertFalse(failure.getMessage().contains(CLIENT_SECRET));
    }

    @Test
    void malformedCredentialFailsCleanlyWithoutSecrets() throws Exception {
        Files.writeString(credentialFile, "{\"clientId\": \"" + CLIENT_ID + "\"}");

        QaCliApiClientException failure = assertThrows(QaCliApiClientException.class,
                () -> client.upsertTask(upsertRequest(null, null)));

        assertTrue(failure.getMessage().startsWith("QA agent credential file is missing clientId or clientSecret: "),
                failure.getMessage());
    }

    private static final String EXTERNAL_KEY = "github/TavallStudios/tavall-content/issues/76";

    private QaCliTaskUpsertRequest upsertRequest(String description, String sourceReference) {
        return new QaCliTaskUpsertRequest(EXTERNAL_KEY, "Replace qa_task_board.py", TaskBoardTaskStatus.ACTIVE,
                description, TaskBoardTaskPriority.NORMAL, "orchestration", "tavall-engineering",
                "github-issue", sourceReference, credentialFile);
    }

    private QaCliDesignRegisterRequest registerRequest(Integer expectedRevision) {
        return new QaCliDesignRegisterRequest(TASK_ID, "QA task board", List.of(
                new org.tavall.qa.domain.TaskBoardDesignCandidate("A", "Board A",
                        "https://qa.tavall.org/a", "fastest to ship"),
                new org.tavall.qa.domain.TaskBoardDesignCandidate("B", "Board B",
                        "https://qa.tavall.org/b", "richest detail",
                        Map.of("theme", List.of("dark", "light")))),
                expectedRevision, credentialFile);
    }

    private static String decidedReviewJson(int revision) {
        return "{\"taskId\":\"" + TASK_ID + "\",\"surface\":\"QA task board\",\"revision\":" + revision
                + ",\"registeredByAccountId\":\"00000000-0000-0000-0000-000000000901\",\"registeredAt\":\"2026-11-26T09:00:00Z\""
                + ",\"winnerKey\":\"C\",\"decidedByAccountId\":\"00000000-0000-0000-0000-000000000901\",\"decidedAt\":\"2026-11-26T10:00:00Z\""
                + ",\"note\":\"Attribution chosen for provenance\""
                + ",\"candidates\":[{\"key\":\"A\",\"name\":\"Board A\",\"previewUrl\":\"https://qa.tavall.org/a\",\"rationale\":\"fast\"}"
                + ",{\"key\":\"B\",\"name\":\"Board B\",\"previewUrl\":\"https://qa.tavall.org/b\",\"rationale\":\"rich\"}"
                + ",{\"key\":\"C\",\"name\":\"Board C\",\"previewUrl\":\"https://qa.tavall.org/c\",\"rationale\":\"rich\"}]}";
    }

    private static String undecidedReviewJson(int revision) {
        return "{\"taskId\":\"" + TASK_ID + "\",\"surface\":\"QA task board\",\"revision\":" + revision
                + ",\"registeredByAccountId\":\"00000000-0000-0000-0000-000000000901\",\"registeredAt\":\"2026-11-26T09:00:00Z\""
                + ",\"candidates\":[{\"key\":\"A\",\"name\":\"Board A\",\"previewUrl\":\"https://qa.tavall.org/a\",\"rationale\":\"fast\"}"
                + ",{\"key\":\"B\",\"name\":\"Board B\",\"previewUrl\":\"https://qa.tavall.org/b\",\"rationale\":\"rich\"}]}";
    }

    private static String firstHeader(RecordedRequest request, String name) {
        List<String> values = request.headers().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private RecordedRequest request(String path) {
        return requests.stream()
                .filter(recorded -> recorded.path().equals(path))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private void record(HttpExchange exchange) throws IOException {
        requests.add(RecordedRequest.from(exchange));
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        record(exchange);
        respond(exchange, tokenStatus, tokenBody);
    }

    private void handleUpsert(HttpExchange exchange) throws IOException {
        record(exchange);
        respond(exchange, upsertStatus, upsertBody);
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        record(exchange);
        String path = exchange.getRequestURI().getRawPath();
        if (path.endsWith("/design-review/candidates")) {
            respond(exchange, candidatesStatus, candidatesBody);
        } else if (path.endsWith("/design-review/ai-review")) {
            respond(exchange, 200, """
                    {"taskId":"%s","revision":4,"aiReviewerModel":"gemini-4-neon-medium",
                     "aiCandidateKey":"A","winnerKey":null}
                    """.formatted(TASK_ID));
        } else if (path.endsWith("/design-review")) {
            respond(exchange, reviewStatus, reviewBody);
        } else {
            respond(exchange, 404, "{}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private record RecordedRequest(String method, String path, Map<String, List<String>> headers, String body) {
        static RecordedRequest from(HttpExchange exchange) throws IOException {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            return new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                    exchange.getRequestHeaders(), new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
