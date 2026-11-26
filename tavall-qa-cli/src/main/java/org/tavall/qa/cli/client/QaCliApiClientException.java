package org.tavall.qa.cli.client;

/** Clean typed failure of a QA execution API exchange; messages are already secret-free. */
public final class QaCliApiClientException extends RuntimeException {
    private final int statusCode;

    public QaCliApiClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public QaCliApiClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
