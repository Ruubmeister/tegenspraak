package nl.rubium.tegenspraak.investigation.control;

import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;

/** A mail or file the team has opened, together with the desktop and app it was found in. */
public record EvidenceItem(Desktop desktop, DesktopApp app, LockableItem item) {

    public EvidenceRef ref() {
        return new EvidenceRef(desktop.id(), app.id(), item.id());
    }

    /** The subject of a mail or the name of a file. */
    public String title() {
        return switch (item) {
            case MailMessage mail -> mail.subject();
            case FileEntry file -> file.name();
        };
    }
}
