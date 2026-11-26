package org.tavall.qa.cli;

import org.tavall.cli.api.command.data.CLICommandInput;

import java.nio.file.Path;
import java.util.Optional;

/** Shared helpers for the {@code qa} CLI commands. */
final class QaCliCommandSupport {

    private QaCliCommandSupport() {
    }

    /** Resolves the optional {@code --credential} option into a path, or null for default resolution. */
    static Path credentialFile(CLICommandInput input) {
        Optional<String> raw = input.findOption("credential");
        if (raw.isEmpty() || raw.get().isBlank()) {
            return null;
        }
        return Path.of(raw.get().trim());
    }
}
