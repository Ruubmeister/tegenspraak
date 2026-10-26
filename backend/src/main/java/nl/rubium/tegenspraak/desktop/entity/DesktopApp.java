package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Optional;

/**
 * An application on a virtual desktop. The "type" property selects the implementation in the case files. Adding a new
 * kind of app means adding a record here, a response in the investigation boundary and a matching frontend component.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MailApp.class, name = "mail"),
    @JsonSubTypes.Type(value = FilesApp.class, name = "files"),
    @JsonSubTypes.Type(value = CalendarApp.class, name = "calendar"),
    @JsonSubTypes.Type(value = PhotosApp.class, name = "photos"),
    @JsonSubTypes.Type(value = BrowserApp.class, name = "browser")
})
public sealed interface DesktopApp permits MailApp, FilesApp, CalendarApp, PhotosApp, BrowserApp {

    String id();

    String title();

    /** The mails, files, events, photos or visited pages of this app. */
    List<? extends LockableItem> items();

    default Optional<LockableItem> findItem(String itemId) {
        return items().stream()
                .filter(item -> item.id().equals(itemId))
                .map(LockableItem.class::cast)
                .findFirst();
    }
}
