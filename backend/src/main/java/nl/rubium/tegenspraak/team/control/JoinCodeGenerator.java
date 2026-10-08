package nl.rubium.tegenspraak.team.control;

import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Join codes are 8 random characters from an alphabet without look-alikes (no 0/O, 1/I), shown as ABCD-EFGH. That gives
 * 32^8 (about 10^12) codes, enough that guessing one is not practical.
 */
@Component
public class JoinCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Accepts what players type: any case, with or without the dash or spaces. */
    public static String normalize(String input) {
        return input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static String format(String code) {
        return code.substring(0, LENGTH / 2) + "-" + code.substring(LENGTH / 2);
    }
}
