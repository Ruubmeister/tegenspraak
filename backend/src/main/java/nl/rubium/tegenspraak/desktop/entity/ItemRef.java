package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Points to an item on a desktop of a case by its ids, for example evidence a secret requires or an attachment a player
 * sent. Case files write it as {@code { desktop, app, item }}.
 */
public record ItemRef(
        @JsonProperty("desktop") String desktopId,
        @JsonProperty("app") String appId,
        @JsonProperty("item") String itemId) {

    public ItemRef {
        if (isBlank(desktopId) || isBlank(appId) || isBlank(itemId)) {
            throw new IllegalArgumentException("An item reference needs a desktop, an app and an item");
        }
    }

    private static boolean isBlank(String id) {
        return id == null || id.isBlank();
    }
}
