package org.tavall.qa.domain.execution;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** A bounded inline Git patch or an existing immutable Storage blob, with its actual content digest. */
public record TaskBoardExecutionDiffData(String sha256, String inlinePatch, URI artifactReference) {
    public TaskBoardExecutionDiffData {
        Objects.requireNonNull(sha256, "sha256");
        if (!sha256.matches("[a-f0-9]{64}") || (inlinePatch == null) == (artifactReference == null)) {
            throw new IllegalArgumentException("A diff requires an exact digest and one content source");
        }
        if (inlinePatch != null && (inlinePatch.getBytes(StandardCharsets.UTF_8).length > 1_048_576
                || !hash(inlinePatch).equals(sha256))) {
            throw new IllegalArgumentException("Inline diff exceeds the bound or differs from its digest");
        }
        if (artifactReference != null && !artifactReference.toString()
                .equals("tavall-storage://artifacts/sha256/" + sha256)) {
            throw new IllegalArgumentException("External diff must reference its immutable Storage identity");
        }
    }

    public static TaskBoardExecutionDiffData inline(String patch) {
        Objects.requireNonNull(patch, "patch");
        return new TaskBoardExecutionDiffData(hash(patch), patch, null);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
