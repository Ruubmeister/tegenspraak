package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;

/** A desktop item a player attached to a message, stored by its ids in the case files. */
@Embeddable
public record ChatAttachment(
        @Column(name = "desktop_id", nullable = false) String desktopId,
        @Column(name = "app_id", nullable = false) String appId,
        @Column(name = "item_id", nullable = false) String itemId) {

    public static ChatAttachment of(ItemRef ref) {
        return new ChatAttachment(ref.desktopId(), ref.appId(), ref.itemId());
    }

    public ItemRef ref() {
        return new ItemRef(desktopId, appId, itemId);
    }
}
