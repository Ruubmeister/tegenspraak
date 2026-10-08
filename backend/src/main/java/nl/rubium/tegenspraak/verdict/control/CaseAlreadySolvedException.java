package nl.rubium.tegenspraak.verdict.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Case is already solved")
public class CaseAlreadySolvedException extends RuntimeException {

    public CaseAlreadySolvedException(String message) {
        super(message);
    }
}
