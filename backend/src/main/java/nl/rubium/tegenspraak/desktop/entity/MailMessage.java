package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** A mail in a mailbox. The folder is free text such as "Inbox" or "Verzonden". A lock is optional. */
public record MailMessage(
        String id,
        @NotBlank String folder,
        @NotBlank String from,
        @NotBlank String to,
        @NotBlank String subject,
        @NotNull LocalDateTime sentAt,
        @NotBlank String body,
        @Valid Lock lock)
        implements LockableItem {}
