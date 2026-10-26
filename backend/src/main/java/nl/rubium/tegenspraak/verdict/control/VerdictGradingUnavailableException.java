package nl.rubium.tegenspraak.verdict.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The LLM could not grade the answers. The attempt is not stored, so it does not count. */
@ResponseStatus(value = HttpStatus.BAD_GATEWAY, reason = "The answers cannot be graded right now")
public class VerdictGradingUnavailableException extends RuntimeException {

    public VerdictGradingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerdictGradingUnavailableException(String message) {
        super(message);
    }
}
