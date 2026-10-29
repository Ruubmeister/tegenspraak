package nl.rubium.tegenspraak.investigation.control;

import nl.rubium.tegenspraak.desktop.entity.BrowserPage;
import nl.rubium.tegenspraak.desktop.entity.CalendarEvent;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.Photo;

/** A desktop item the team has opened, together with the desktop and app it was found in. */
public record EvidenceItem(Desktop desktop, DesktopApp app, LockableItem item) {

    /** The subject of a mail, the name of a file or the title of an event, photo or page. */
    public String title() {
        return switch (item) {
            case MailMessage mail -> mail.subject();
            case FileEntry file -> file.name();
            case CalendarEvent event -> event.title();
            case Photo photo -> photo.title();
            case BrowserPage page -> page.title();
        };
    }
}
