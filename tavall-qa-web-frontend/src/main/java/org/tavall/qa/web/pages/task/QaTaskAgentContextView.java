package org.tavall.qa.web.pages.task;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * View projection of the execution context utilized by an agent working on a QA task.
 */
public record QaTaskAgentContextView(
        UUID agentAccountId,
        String agentLabel,
        String colorToken,
        UUID runId,
        String status,
        Instant recordedAt,
        String nextAction,
        List<String> inspectedFiles,
        List<String> modifiedFiles,
        String diffSha256,
        String diffPreview,
        List<String> documents,
        List<String> evidence
) {
    public static final int MAX_LIST_ENTRIES = 64;
    public static final int MAX_DIFF_PREVIEW_BYTES = 16 * 1024; // 16 KiB

    /** Small fixed palette of accessible accent colors. */
    public static final List<String> PALETTE = List.of(
            "#38bdf8", // Sky blue
            "#34d399", // Emerald
            "#fbbf24", // Amber
            "#a78bfa", // Purple
            "#f472b6", // Pink
            "#2dd4bf", // Teal
            "#fb923c", // Orange
            "#818cf8"  // Indigo
    );

    public QaTaskAgentContextView {
        Objects.requireNonNull(agentAccountId, "agentAccountId");
        Objects.requireNonNull(agentLabel, "agentLabel");
        Objects.requireNonNull(recordedAt, "recordedAt");
        colorToken = (colorToken == null || colorToken.isBlank())
                ? colorTokenFor(agentAccountId)
                : colorToken;
        status = status == null ? "" : status;
        nextAction = nextAction == null ? "" : nextAction;
        diffSha256 = diffSha256 == null ? "" : diffSha256;
        diffPreview = truncatePreview(diffPreview, MAX_DIFF_PREVIEW_BYTES);
        inspectedFiles = boundList(inspectedFiles, MAX_LIST_ENTRIES);
        modifiedFiles = boundList(modifiedFiles, MAX_LIST_ENTRIES);
        documents = boundList(documents, MAX_LIST_ENTRIES);
        evidence = boundList(evidence, MAX_LIST_ENTRIES);
    }

    public static QaTaskAgentContextView of(
            UUID agentAccountId,
            String agentLabel,
            UUID runId,
            String status,
            Instant recordedAt,
            String nextAction,
            List<String> inspectedFiles,
            List<String> modifiedFiles,
            String diffSha256,
            String diffPreview,
            List<String> documents,
            List<String> evidence
    ) {
        return new QaTaskAgentContextView(
                agentAccountId,
                agentLabel,
                colorTokenFor(agentAccountId),
                runId,
                status,
                recordedAt,
                nextAction,
                inspectedFiles,
                modifiedFiles,
                diffSha256,
                diffPreview,
                documents,
                evidence
        );
    }

    public static String colorTokenFor(UUID accountId) {
        if (accountId == null) {
            return PALETTE.get(0);
        }
        int hash = Long.hashCode(accountId.getMostSignificantBits() ^ accountId.getLeastSignificantBits());
        return PALETTE.get(Math.floorMod(hash, PALETTE.size()));
    }

    private static String truncatePreview(String preview, int maxBytes) {
        if (preview == null || preview.isEmpty()) {
            return "";
        }
        byte[] bytes = preview.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return preview;
        }
        String truncated = new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);
        return truncated + "\n... (truncated to 16 KiB)";
    }

    private static List<String> boundList(List<String> list, int maxEntries) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<String> nonNullLimited = list.stream()
                .filter(Objects::nonNull)
                .limit(maxEntries)
                .toList();
        return List.copyOf(nonNullLimited);
    }
}
