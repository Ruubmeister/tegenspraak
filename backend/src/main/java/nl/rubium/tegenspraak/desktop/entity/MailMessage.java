package nl.rubium.tegenspraak.desktop.entity;

import java.time.LocalDateTime;

/** A mail in a mailbox. The folder is free text such as "Inbox" or "Verzonden". */
public record MailMessage(
        String id, String folder, String from, String to, String subject, LocalDateTime sentAt, String body) {}
