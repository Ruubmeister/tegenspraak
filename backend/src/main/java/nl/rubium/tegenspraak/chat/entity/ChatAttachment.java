package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A mail or file a player attached to a message, by its ids in the case files. */
@Embeddable
public record ChatAttachment(
        @Column(name = "desktop_id", nullable = false) String desktopId,
        @Column(name = "app_id", nullable = false) String appId,
        @Column(name = "item_id", nullable = false) String itemId) {}
