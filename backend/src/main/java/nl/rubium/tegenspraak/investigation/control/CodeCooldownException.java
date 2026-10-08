package nl.rubium.tegenspraak.investigation.control;

import java.time.Duration;

/** Too many wrong codes for one target; the team has to wait before trying again. */
public class CodeCooldownException extends RuntimeException {

    private final Duration retryAfter;

    public CodeCooldownException(String target, Duration retryAfter) {
        super("Too many wrong codes for " + target + ", retry after " + retryAfter);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
