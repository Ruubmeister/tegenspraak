package nl.rubium.tegenspraak.verdict.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The submitted answers do not match the questions of the case. */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Every question needs exactly one answer")
public class InvalidVerdictException extends RuntimeException {

    public InvalidVerdictException(String message) {
        super(message);
    }
}
