package org.tavall.qa.domain.mirror;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Mirror configuration settings defining the mirror root on NFS.
 */
public record TaskBoardFileMirrorSettings(
        Path mirrorRoot
) {
    public static final String DEFAULT_MIRROR_ROOT_ENV = "TAVALL_QA_FILE_MIRROR_ROOT";

    public TaskBoardFileMirrorSettings {
        Objects.requireNonNull(mirrorRoot, "mirrorRoot");
    }

    public static TaskBoardFileMirrorSettings createDefault() {
        String env = System.getenv(DEFAULT_MIRROR_ROOT_ENV);
        if (env != null && !env.isBlank()) {
            return new TaskBoardFileMirrorSettings(Path.of(env.trim()));
        }
        return new TaskBoardFileMirrorSettings(Path.of("/srv/dev-storage/qa/mirror"));
    }

    public static TaskBoardFileMirrorSettings fromEnvironmentOrDefault(Path defaultRoot) {
        String env = System.getenv(DEFAULT_MIRROR_ROOT_ENV);
        if (env != null && !env.isBlank()) {
            return new TaskBoardFileMirrorSettings(Path.of(env.trim()));
        }
        return new TaskBoardFileMirrorSettings(Objects.requireNonNull(defaultRoot, "defaultRoot"));
    }
}
