package nl.rubium.tegenspraak.verdict.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The team has no attempts left for this case")
public class NoAttemptsLeftException extends RuntimeException {

    public NoAttemptsLeftException(String message) {
        super(message);
    }
}
