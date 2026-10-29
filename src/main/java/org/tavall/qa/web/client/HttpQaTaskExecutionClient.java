package org.tavall.qa.web.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard HTTP implementation of {@link QaTaskExecutionClient} invoking the execution API.
 */
public final class HttpQaTaskExecutionClient implements QaTaskExecutionClient {

    private final HttpClient http;
    private final URI baseUri;
    private final Duration requestTimeout;

    public HttpQaTaskExecutionClient(HttpClient http, URI baseUri, Duration requestTimeout) {
        this.http = Objects.requireNonNull(http, "http");
        Objects.requireNonNull(baseUri, "baseUri");
        String base = baseUri.toString();
        this.baseUri = URI.create(base.endsWith("/") ? base.substring(0, base.length() - 1) : base);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    public HttpQaTaskExecutionClient(URI baseUri) {
        this(HttpClient.newHttpClient(), baseUri, Duration.ofSeconds(5));
    }

    @Override
    public Optional<QaExecutionLeaseResponse> fetchLease(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        URI uri = URI.create(baseUri + "/api/execution/tasks/" + taskId + "/lease");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return Optional.of(parseLease(response.body()));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<QaExecutionCheckpointResponse> fetchCheckpoints(UUID taskId, UUID runId) {
        Objects.requireNonNull(taskId, "taskId");
        String uriStr = baseUri + "/api/execution/tasks/" + taskId + "/checkpoints"
                + (runId != null ? "?runId=" + runId : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uriStr))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return parseCheckpoints(response.body());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Long extractJsonLong(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private static QaExecutionLeaseResponse parseLease(String json) {
        String taskIdStr = extractJsonString(json, "taskId");
        String runIdStr = extractJsonString(json, "runId");
        String leaseIdStr = extractJsonString(json, "leaseId");
        String ownerAccountIdStr = extractJsonString(json, "ownerAccountId");
        Long revision = extractJsonLong(json, "revision");
        Long checkpointSequence = extractJsonLong(json, "checkpointSequence");
        String taskFingerprint = extractJsonString(json, "taskFingerprint");
        String expiresAtStr = extractJsonString(json, "expiresAt");

        return new QaExecutionLeaseResponse(
                taskIdStr != null ? UUID.fromString(taskIdStr) : UUID.randomUUID(),
                runIdStr != null ? UUID.fromString(runIdStr) : UUID.randomUUID(),
                leaseIdStr != null ? UUID.fromString(leaseIdStr) : UUID.randomUUID(),
                ownerAccountIdStr != null ? UUID.fromString(ownerAccountIdStr) : UUID.randomUUID(),
                revision != null ? revision : 0L,
                checkpointSequence != null ? checkpointSequence : 0L,
                taskFingerprint != null ? taskFingerprint : "",
                expiresAtStr != null ? Instant.parse(expiresAtStr) : Instant.now()
        );
    }

    private static List<QaExecutionCheckpointResponse> parseCheckpoints(String json) {
        List<QaExecutionCheckpointResponse> list = new ArrayList<>();
        Pattern objectPattern = Pattern.compile("\\{[^{}]+?\\}");
        Matcher matcher = objectPattern.matcher(json);
        while (matcher.find()) {
            String item = matcher.group();
            String checkpointIdStr = extractJsonString(item, "checkpointId");
            String taskIdStr = extractJsonString(item, "taskId");
            String runIdStr = extractJsonString(item, "runId");
            String leaseIdStr = extractJsonString(item, "leaseId");
            Long sequence = extractJsonLong(item, "sequence");
            String taskFingerprint = extractJsonString(item, "taskFingerprint");
            String payloadSha256 = extractJsonString(item, "payloadSha256");
            String providerId = extractJsonString(item, "providerId");
            String status = extractJsonString(item, "status");
            String recordedByAccountIdStr = extractJsonString(item, "recordedByAccountId");
            String recordedAtStr = extractJsonString(item, "recordedAt");

            if (checkpointIdStr != null && providerId != null) {
                list.add(new QaExecutionCheckpointResponse(
                        UUID.fromString(checkpointIdStr),
                        taskIdStr != null ? UUID.fromString(taskIdStr) : UUID.randomUUID(),
                        runIdStr != null ? UUID.fromString(runIdStr) : UUID.randomUUID(),
                        leaseIdStr != null ? UUID.fromString(leaseIdStr) : UUID.randomUUID(),
                        sequence != null ? sequence : 1L,
                        taskFingerprint != null ? taskFingerprint : "",
                        payloadSha256 != null ? payloadSha256 : "",
                        providerId,
                        status != null ? status : "RUNNING",
                        recordedByAccountIdStr != null ? UUID.fromString(recordedByAccountIdStr) : UUID.randomUUID(),
                        recordedAtStr != null ? Instant.parse(recordedAtStr) : Instant.now()
                ));
            }
        }
        return list;
    }
}
