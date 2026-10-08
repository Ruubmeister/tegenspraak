package nl.rubium.tegenspraak.investigation.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_CONTENT, reason = "Wrong code")
public class WrongCodeException extends RuntimeException {

    public WrongCodeException(String target) {
        super("Wrong code for " + target);
    }
}
