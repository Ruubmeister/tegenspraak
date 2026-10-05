package nl.rubium.tegenspraak.casefile.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CaseContentNotFoundException extends RuntimeException {

    public CaseContentNotFoundException(String message) {
        super(message);
    }
}
