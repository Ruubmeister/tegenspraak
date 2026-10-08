package nl.rubium.tegenspraak.team.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Also thrown for teams the player is not a member of, so team ids reveal nothing to outsiders. */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Team not found")
public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(String message) {
        super(message);
    }
}
