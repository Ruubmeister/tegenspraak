package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nl.rubium.tegenspraak.investigation.control.EvidenceRef;

/** A mail or file the team has opened, by its ids. */
public record AttachmentRequest(
        @NotBlank @Size(max = 100) String desktopId,
        @NotBlank @Size(max = 100) String appId,
        @NotBlank @Size(max = 100) String itemId) {

    EvidenceRef toRef() {
        return new EvidenceRef(desktopId, appId, itemId);
    }
}
