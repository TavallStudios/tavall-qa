package org.tavall.qa.cli.client;

/** Replaces live secrets and short-lived tokens with {@code <redacted>} before any text escapes. */
public final class QaCliSecretRedactor {
    public static final String REDACTED = "<redacted>";

    private QaCliSecretRedactor() {
    }

    public static String redact(String text, String... secrets) {
        String result = text == null ? "" : text;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                result = result.replace(secret, REDACTED);
            }
        }
        return result;
    }
}
