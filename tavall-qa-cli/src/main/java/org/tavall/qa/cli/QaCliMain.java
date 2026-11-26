package org.tavall.qa.cli;

import org.tavall.cli.runtime.TavallCliApplication;

import java.io.PrintStream;

/**
 * Runnable entry point of the self-contained {@code tavall-qa-cli} artifact.
 *
 * <p>Delegates to the unified Tavall CLI application ({@link TavallCliApplication#execute(String[],
 * PrintStream, PrintStream)}), which boots the CLI DI core and ServiceLoader-discovers {@code
 * META-INF/services/org.tavall.cli.api.bootstrap.ICLIBootstrap} — including this module's {@link
 * QaCLIBootstrap} — before routing the typed {@code qa} commands.</p>
 */
public final class QaCliMain {

    private QaCliMain() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Testable dispatch returning the process exit code instead of terminating the JVM. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        return TavallCliApplication.execute(args, out, err);
    }
}
