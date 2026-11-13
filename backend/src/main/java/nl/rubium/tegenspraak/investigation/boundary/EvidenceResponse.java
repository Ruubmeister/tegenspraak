package nl.rubium.tegenspraak.investigation.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.rubium.tegenspraak.desktop.entity.BrowserPage;
import nl.rubium.tegenspraak.desktop.entity.CalendarEvent;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;

/** An opened desktop item that a player can attach to a chat message. Only the title, never the content. */
public record EvidenceResponse(
        String desktopId, String appId, String itemId, Kind kind, String title, String ownerName) {

    public enum Kind {
        @JsonProperty("mail")
        MAIL,
        @JsonProperty("file")
        FILE,
        @JsonProperty("event")
        EVENT,
        @JsonProperty("photo")
        PHOTO,
        @JsonProperty("webpage")
        WEBPAGE
    }

    public static EvidenceResponse from(EvidenceItem evidence) {
        Kind kind = switch (evidence.item()) {
            case MailMessage mail -> Kind.MAIL;
            case FileEntry file -> Kind.FILE;
            case CalendarEvent event -> Kind.EVENT;
            case Photo photo -> Kind.PHOTO;
            case BrowserPage page -> Kind.WEBPAGE;
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
