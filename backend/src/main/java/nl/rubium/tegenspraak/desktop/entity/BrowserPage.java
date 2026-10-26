package nl.rubium.tegenspraak.desktop.entity;

import java.time.LocalDateTime;

/** A visit in the browsing history, with the text of the page. A lock is optional and hides the text. */
public record BrowserPage(String id, String url, String title, LocalDateTime visitedAt, String content, Lock lock)
        implements LockableItem {

    public BrowserPage {
        if (url == null || url.isBlank() || title == null || title.isBlank() || visitedAt == null) {
            throw new IllegalArgumentException("Page '" + id + "' needs a url, a title and a visit time");
        }
    }
}
