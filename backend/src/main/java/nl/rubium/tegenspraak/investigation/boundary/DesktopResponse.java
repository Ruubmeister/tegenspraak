package nl.rubium.tegenspraak.investigation.boundary;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.List;
import nl.rubium.tegenspraak.desktop.entity.BrowserApp;
import nl.rubium.tegenspraak.desktop.entity.BrowserPage;
import nl.rubium.tegenspraak.desktop.entity.CalendarApp;
import nl.rubium.tegenspraak.desktop.entity.CalendarEvent;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import nl.rubium.tegenspraak.desktop.entity.PhotosApp;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;

/**
 * A desktop as a team sees it. A locked desktop has no apps; a locked item shows its title but no content. Locked
 * content never leaves the server. Photos carry no image; the browser loads it from the image endpoint, which checks
 * the lock as well.
 */
public record DesktopResponse(
        String id,
        String ownerName,
        OwnerType ownerType,
        String description,
        LockResponse lock,
        List<AppResponse> apps) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = MailAppResponse.class, name = "mail"),
        @JsonSubTypes.Type(value = FilesAppResponse.class, name = "files"),
        @JsonSubTypes.Type(value = CalendarAppResponse.class, name = "calendar"),
        @JsonSubTypes.Type(value = PhotosAppResponse.class, name = "photos"),
        @JsonSubTypes.Type(value = BrowserAppResponse.class, name = "browser")
    })
    public sealed interface AppResponse
            permits MailAppResponse, FilesAppResponse, CalendarAppResponse, PhotosAppResponse, BrowserAppResponse {}

    public record MailAppResponse(String id, String title, List<MailResponse> messages) implements AppResponse {}

    public record FilesAppResponse(String id, String title, List<FileResponse> files) implements AppResponse {}

    public record CalendarAppResponse(String id, String title, List<EventResponse> events) implements AppResponse {}

    public record PhotosAppResponse(String id, String title, List<PhotoResponse> photos) implements AppResponse {}

    public record BrowserAppResponse(String id, String title, List<PageResponse> pages) implements AppResponse {}

    public record MailResponse(
            String id,
            String folder,
            String from,
            String to,
            String subject,
            LocalDateTime sentAt,
            String body,
            LockResponse lock) {}

    public record FileResponse(String id, String folder, String name, String content, LockResponse lock) {}

    public record EventResponse(
            String id,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            String location,
            List<String> attendees,
            String notes,
            LockResponse lock) {}

    public record PhotoResponse(
            String id, String album, String title, LocalDateTime takenAt, String description, LockResponse lock) {}

    public record PageResponse(
            String id, String url, String title, LocalDateTime visitedAt, String content, LockResponse lock) {}

    static DesktopResponse from(Desktop desktop, TeamProgress progress) {
        boolean open = progress.isOpen(desktop);
        return new DesktopResponse(
                desktop.id(),
                desktop.ownerName(),
                desktop.ownerType(),
                desktop.description(),
                LockResponse.whileLocked(desktop.lock(), open),
                open
                        ? desktop.apps().stream()
                                .map(app -> toApp(desktop, app, progress))
                                .toList()
                        : List.of());
    }

    private static AppResponse toApp(Desktop desktop, DesktopApp app, TeamProgress progress) {
        return switch (app) {
            case MailApp mailApp ->
                new MailAppResponse(
                        mailApp.id(),
                        mailApp.title(),
                        mailApp.messages().stream()
                                .map(mail -> toMail(mail, progress.isOpen(desktop, app, mail)))
                                .toList());
            case FilesApp filesApp ->
                new FilesAppResponse(
                        filesApp.id(),
                        filesApp.title(),
                        filesApp.files().stream()
                                .map(file -> toFile(file, progress.isOpen(desktop, app, file)))
                                .toList());
            case CalendarApp calendarApp ->
                new CalendarAppResponse(
                        calendarApp.id(),
                        calendarApp.title(),
                        calendarApp.events().stream()
                                .map(event -> toEvent(event, progress.isOpen(desktop, app, event)))
                                .toList());
            case PhotosApp photosApp ->
                new PhotosAppResponse(
                        photosApp.id(),
                        photosApp.title(),
                        photosApp.photos().stream()
                                .map(photo -> toPhoto(photo, progress.isOpen(desktop, app, photo)))
                                .toList());
            case BrowserApp browserApp ->
                new BrowserAppResponse(
                        browserApp.id(),
                        browserApp.title(),
                        browserApp.pages().stream()
                                .map(page -> toPage(page, progress.isOpen(desktop, app, page)))
                                .toList());
        };
    }

    private static MailResponse toMail(MailMessage mail, boolean open) {
        return new MailResponse(
                mail.id(),
                mail.folder(),
                mail.from(),
                mail.to(),
                mail.subject(),
                mail.sentAt(),
                open ? mail.body() : null,
                LockResponse.whileLocked(mail.lock(), open));
    }

    private static FileResponse toFile(FileEntry file, boolean open) {
        return new FileResponse(
                file.id(),
                file.folder(),
                file.name(),
                open ? file.content() : null,
                LockResponse.whileLocked(file.lock(), open));
    }

    private static EventResponse toEvent(CalendarEvent event, boolean open) {
        return new EventResponse(
                event.id(),
                event.title(),
                event.start(),
                event.end(),
                open ? event.location() : null,
                open ? event.attendees() : List.of(),
                open ? event.notes() : null,
                LockResponse.whileLocked(event.lock(), open));
    }

    private static PhotoResponse toPhoto(Photo photo, boolean open) {
        return new PhotoResponse(
                photo.id(),
                photo.album(),
                photo.title(),
                photo.takenAt(),
                open ? photo.description() : null,
                LockResponse.whileLocked(photo.lock(), open));
    }

    private static PageResponse toPage(BrowserPage page, boolean open) {
        return new PageResponse(
                page.id(),
                page.url(),
                page.title(),
                page.visitedAt(),
                open ? page.content() : null,
                LockResponse.whileLocked(page.lock(), open));
    }
}
