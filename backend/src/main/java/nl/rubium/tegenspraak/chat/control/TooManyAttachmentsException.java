package nl.rubium.tegenspraak.chat.control;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Too many attachments")
public class TooManyAttachmentsException extends RuntimeException {

    public TooManyAttachmentsException(int count) {
        super("A message has at most " + ChatService.MAX_ATTACHMENTS + " attachments, got " + count);
    }
}
