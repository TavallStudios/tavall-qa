package org.tavall.qa.cli.github;

/**
 * Typed failure of the GitHub issue reader.
 *
 * <p>Messages describe the failed REST path and the command exit status only; tokens and
 * credentials are redacted before the message is created.</p>
 */
public final class QaGithubReaderException extends RuntimeException {

    public QaGithubReaderException(String message) {
        super(message);
    }

    public QaGithubReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
