package org.tavall.qa.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Stable fingerprint for the task fields whose changes invalidate reviewed evidence. */
public final class TaskBoardTaskFingerprint {
    private TaskBoardTaskFingerprint() {
    }

    public static String compute(TaskBoardTask task) {
        Objects.requireNonNull(task, "task");
        List<String> values = List.of(
                task.projectKey(),
                task.sectionKey(),
                task.title(),
                task.publicTitle(),
                task.description(),
                task.publicSummary(),
                task.priority().name(),
                task.audience().name(),
                Integer.toString(task.weight()),
                Boolean.toString(task.hostValidationRequired()),
                Boolean.toString(task.qaQcEligible()),
                task.sourceType(),
                task.sourceReference(),
                Integer.toString(task.qaRevision())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", exception);
        }
    }
}
