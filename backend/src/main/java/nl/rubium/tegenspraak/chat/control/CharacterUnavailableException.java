package nl.rubium.tegenspraak.chat.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The LLM behind a character could not produce a reply. */
@ResponseStatus(value = HttpStatus.BAD_GATEWAY, reason = "Character is not available right now")
public class CharacterUnavailableException extends RuntimeException {

    public CharacterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CharacterUnavailableException(String message) {
        super(message);
    }
}
