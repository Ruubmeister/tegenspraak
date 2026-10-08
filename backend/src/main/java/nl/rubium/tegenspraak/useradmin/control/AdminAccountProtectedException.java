package nl.rubium.tegenspraak.useradmin.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The admin changes their own password through the account page, never through user management. */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The admin account cannot be managed")
public class AdminAccountProtectedException extends RuntimeException {

    public AdminAccountProtectedException() {
        super("The admin account cannot be managed");
    }
}
