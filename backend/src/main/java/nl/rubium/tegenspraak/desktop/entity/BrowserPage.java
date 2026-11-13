package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** A visit in the browsing history, with the text of the page. A lock is optional and hides the text. */
public record BrowserPage(
        String id,
        @NotBlank String url,
        @NotBlank String title,
        @NotNull LocalDateTime visitedAt,
        @NotBlank String content,
        @Valid Lock lock)
        implements LockableItem {}
