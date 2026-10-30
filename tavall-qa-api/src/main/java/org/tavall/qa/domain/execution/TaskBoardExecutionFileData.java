package org.tavall.qa.domain.execution;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Repository-relative source identity and inspected or modified class/function signatures. */
public record TaskBoardExecutionFileData(String path, String sha256, List<String> symbols) {
    public TaskBoardExecutionFileData {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sha256, "sha256");
        symbols = List.copyOf(symbols);
        if (path.isBlank() || path.length() > 1024 || path.startsWith("/") || path.contains("\\")
                || path.chars().anyMatch(Character::isISOControl)
                || Arrays.stream(path.split("/", -1)).anyMatch(part -> part.isEmpty() || part.equals(".") || part.equals(".."))
                || !sha256.matches("[a-f0-9]{64}") || symbols.size() > 512
                || symbols.stream().anyMatch(symbol -> symbol.isBlank() || symbol.length() > 1024)) {
            throw new IllegalArgumentException("Checkpoint files require contained repository paths and bounded symbol identities");
        }
    }
}
