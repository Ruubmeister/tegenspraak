package nl.rubium.tegenspraak.desktop.entity;

import java.util.Locale;

/**
 * A code that must be entered before a desktop, mail or file opens. The hint is shown to the player; the code never
 * leaves the server. Codes are compared without case and whitespace, so "Kat 2019" matches "kat2019".
 */
public record Lock(String code, String hint) {

    public Lock {
        if (code == null || normalize(code).isEmpty()) {
            throw new IllegalArgumentException("A lock needs a code");
        }
        if (hint == null || hint.isBlank()) {
            throw new IllegalArgumentException("A lock needs a hint");
        }
    }

    public boolean opensWith(String attempt) {
        return attempt != null && normalize(code).equals(normalize(attempt));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }
}
