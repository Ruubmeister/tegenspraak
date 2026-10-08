package nl.rubium.tegenspraak.investigation.boundary;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.List;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;

/**
 * A desktop as a team sees it. A locked desktop has no apps; a locked mail or file shows its title but no content.
 * Locked content never leaves the server.
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
        @JsonSubTypes.Type(value = FilesAppResponse.class, name = "files")
    })
    public sealed interface AppResponse permits MailAppResponse, FilesAppResponse {}

    public record MailAppResponse(String id, String title, List<MailResponse> messages) implements AppResponse {}

    public record FilesAppResponse(String id, String title, List<FileResponse> files) implements AppResponse {}

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
}
