package nl.rubium.tegenspraak.auth.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Username is already taken")
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username is already taken: " + username);
    }
}
