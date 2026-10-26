package nl.rubium.tegenspraak.investigation.boundary;

import nl.rubium.tegenspraak.desktop.entity.BrowserPage;
import nl.rubium.tegenspraak.desktop.entity.CalendarEvent;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;

/** An opened desktop item that a player can attach to a chat message. Only the title, never the content. */
public record EvidenceResponse(
        String desktopId, String appId, String itemId, String kind, String title, String ownerName) {

    public static EvidenceResponse from(EvidenceItem evidence) {
        String kind = switch (evidence.item()) {
            case MailMessage mail -> "mail";
            case FileEntry file -> "file";
            case CalendarEvent event -> "event";
            case Photo photo -> "photo";
            case BrowserPage page -> "webpage";
        };

        return new EvidenceResponse(
                evidence.desktop().id(),
                evidence.app().id(),
                evidence.item().id(),
                kind,
                evidence.title(),
                evidence.desktop().ownerName());
    }
}
