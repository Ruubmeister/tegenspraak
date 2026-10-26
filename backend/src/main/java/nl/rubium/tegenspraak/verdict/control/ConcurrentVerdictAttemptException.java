package nl.rubium.tegenspraak.verdict.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A teammate submitted an attempt while this one was being graded. */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "A teammate submitted an attempt at the same time")
public class ConcurrentVerdictAttemptException extends RuntimeException {

    public ConcurrentVerdictAttemptException(String message, Throwable cause) {
        super(message, cause);
    }
}
