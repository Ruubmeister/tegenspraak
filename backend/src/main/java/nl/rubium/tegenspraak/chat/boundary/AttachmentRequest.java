package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;

/** A desktop item the team has opened, such as a mail, file or photo, by its ids. */
public record AttachmentRequest(
        @NotBlank @Size(max = 100) String desktopId,
        @NotBlank @Size(max = 100) String appId,
        @NotBlank @Size(max = 100) String itemId) {

    ItemRef toRef() {
        return new ItemRef(desktopId, appId, itemId);
    }
}
