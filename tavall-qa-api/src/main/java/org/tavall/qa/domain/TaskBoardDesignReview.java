package org.tavall.qa.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Task-owned design candidates, an immutable attributed human decision, and an optional provisional
 * AI review. An approved AI review can make the task deployable without creating a synthetic human decision.
 */
public record TaskBoardDesignReview(UUID taskId, String surface, List<TaskBoardDesignCandidate> candidates, int revision,
                                    UUID registeredByAccountId, Instant registeredAt,
                                    String winnerKey, UUID decidedByAccountId, Instant decidedAt, String note,
                                    String aiReviewerModel, UUID aiReviewerAccountId, String aiReviewerAgentIdentity,
                                    Instant aiReviewedAt, String aiCandidateKey, String aiRationale,
                                    List<String> aiEvidenceRefs, List<String> aiArtifactRefs, String aiScreenshotArtifactRef) {

    public static final Set<String> APPROVED_AI_REVIEWER_MODELS = Set.of("claude-opus", "gemini-4-neon");

    public TaskBoardDesignReview {
        Objects.requireNonNull(taskId, "taskId");
        surface = Objects.requireNonNull(surface, "surface").trim();
        if (surface.isEmpty() || surface.length() > 200) throw new IllegalArgumentException("Surface is blank or too long");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        if (candidates.size() < 2 || candidates.size() > 4
                || candidates.stream().map(TaskBoardDesignCandidate::key).distinct().count() != candidates.size()) {
            throw new IllegalArgumentException("A design review requires 2–4 candidates with unique keys");
        }
        if (revision < 1) throw new IllegalArgumentException("Review revision must be positive");
        Objects.requireNonNull(registeredByAccountId, "registeredByAccountId");
        Objects.requireNonNull(registeredAt, "registeredAt");
        note = note == null ? "" : note.trim();
        if (note.length() > 4000) throw new IllegalArgumentException("Decision note exceeds 4000 characters");
        if (winnerKey == null) {
            if (decidedByAccountId != null || decidedAt != null || !note.isEmpty()) {
                throw new IllegalArgumentException("Only decided reviews may carry decision details");
            }
        } else {
            String winner = winnerKey;
            if (candidates.stream().noneMatch(candidate -> candidate.key().equals(winner))) {
                throw new IllegalArgumentException("Winner must be a registered candidate");
            }
            Objects.requireNonNull(decidedByAccountId, "decidedByAccountId");
            Objects.requireNonNull(decidedAt, "decidedAt");
        }

        aiReviewerModel = normalizeOptional(aiReviewerModel);
        aiReviewerAgentIdentity = normalizeOptional(aiReviewerAgentIdentity);
        aiCandidateKey = normalizeKey(aiCandidateKey);
        aiRationale = normalizeOptional(aiRationale);
        aiEvidenceRefs = normalizeRefs(aiEvidenceRefs);
        aiArtifactRefs = normalizeRefs(aiArtifactRefs);
        aiScreenshotArtifactRef = normalizeOptional(aiScreenshotArtifactRef);
        boolean hasAiReview = aiReviewerModel != null || aiReviewerAccountId != null || aiReviewerAgentIdentity != null
                || aiReviewedAt != null || aiCandidateKey != null || aiRationale != null
                || !aiEvidenceRefs.isEmpty() || !aiArtifactRefs.isEmpty() || aiScreenshotArtifactRef != null;
        if (hasAiReview) {
            if (!isApprovedAiReviewerModel(aiReviewerModel) || aiReviewerModel.length() > 128) {
                throw new IllegalArgumentException("Approved AI reviewer model required: " + APPROVED_AI_REVIEWER_MODELS);
            }
            if (aiReviewerAccountId == null) {
                throw new IllegalArgumentException("AI design review requires reviewerAccountId");
            }
            if (aiReviewerAgentIdentity == null || aiReviewerAgentIdentity.length() > 255) {
                throw new IllegalArgumentException("AI design review requires a reviewerAgentIdentity of at most 255 characters");
            }
            if (aiReviewedAt == null) {
                throw new IllegalArgumentException("AI design review requires reviewedAt");
            }
            if (aiCandidateKey == null) {
                throw new IllegalArgumentException("AI design review requires candidateKey");
            }
            String provisionalCandidate = aiCandidateKey;
            if (candidates.stream().noneMatch(candidate -> candidate.key().equals(provisionalCandidate))) {
                throw new IllegalArgumentException("AI design review candidate must be one of the registered candidates");
            }
            if (aiRationale == null) {
                throw new IllegalArgumentException("AI design review requires rationale");
            }
            if (aiEvidenceRefs.isEmpty()) {
                throw new IllegalArgumentException("AI design review requires at least one evidence ref");
            }
        }
    }

    /** Backward-compatible constructor for pre-AI-review call sites. */
    public TaskBoardDesignReview(UUID taskId, String surface, List<TaskBoardDesignCandidate> candidates, int revision,
                                 UUID registeredByAccountId, Instant registeredAt,
                                 String winnerKey, UUID decidedByAccountId, Instant decidedAt, String note) {
        this(taskId, surface, candidates, revision, registeredByAccountId, registeredAt,
                winnerKey, decidedByAccountId, decidedAt, note,
                null, null, null, null, null, null, List.of(), List.of(), null);
    }

    public TaskBoardDesignReview(UUID taskId, List<TaskBoardDesignCandidate> candidates, int revision,
                                 UUID registeredByAccountId, Instant registeredAt, String winnerKey,
                                 UUID decidedByAccountId, Instant decidedAt, String note) {
        this(taskId, "Unspecified web surface", candidates, revision, registeredByAccountId, registeredAt,
                winnerKey, decidedByAccountId, decidedAt, note);
    }

    public static TaskBoardDesignReview register(UUID taskId, String surface, List<TaskBoardDesignCandidate> candidates,
                                                int revision, UUID actor, Instant now) {
        return new TaskBoardDesignReview(taskId, surface, candidates, revision, actor, now, null, null, null, "");
    }

    public boolean decided() {
        return winnerKey != null;
    }

    /** Provisional deployability: human decided or approved AI review present. */
    public boolean deployable() {
        return decided() || aiReviewed();
    }

    public boolean aiReviewed() {
        return aiReviewerModel != null;
    }

    public static boolean isApprovedAiReviewerModel(String model) {
        if (model == null) return false;
        String identifier = model.trim().toLowerCase(Locale.ROOT);
        return APPROVED_AI_REVIEWER_MODELS.stream().anyMatch(family ->
                identifier.equals(family) || identifier.startsWith(family + "-"));
    }

    public TaskBoardDesignReview select(String key, UUID actor, String decisionNote, Instant now) {
        if (decided()) {
            throw new IllegalStateException("Design review already has a human decision");
        }
        return new TaskBoardDesignReview(taskId, surface, candidates, revision + 1, registeredByAccountId, registeredAt,
                key, actor, now, decisionNote,
                aiReviewerModel, aiReviewerAccountId, aiReviewerAgentIdentity, aiReviewedAt, aiCandidateKey,
                aiRationale, aiEvidenceRefs, aiArtifactRefs, aiScreenshotArtifactRef);
    }

    public TaskBoardDesignReview withAiReview(String reviewerModel, UUID reviewerAccountId, String reviewerAgentIdentity,
                                              Instant reviewedAt, String candidateKey, String rationale,
                                              List<String> evidenceRefs, List<String> artifactRefs,
                                              String screenshotArtifactRef) {
        if (decided()) {
            throw new IllegalStateException("Design review already has a human decision");
        }
        return new TaskBoardDesignReview(taskId, surface, candidates, revision + 1, registeredByAccountId, registeredAt,
                winnerKey, decidedByAccountId, decidedAt, note,
                reviewerModel, reviewerAccountId, reviewerAgentIdentity, reviewedAt, candidateKey,
                rationale, evidenceRefs, artifactRefs, screenshotArtifactRef);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeKey(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static List<String> normalizeRefs(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        return refs.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
