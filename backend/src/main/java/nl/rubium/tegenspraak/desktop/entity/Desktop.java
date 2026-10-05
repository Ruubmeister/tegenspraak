package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import java.util.List;

/**
 * Virtual desktop of a person or organisation that the player can search for evidence. The id is the file name of the
 * desktop definition and is injected while loading.
 */
public record Desktop(
        @JacksonInject("id") String id,
        String ownerName,
        OwnerType ownerType,
        String description,
        List<DesktopApp> apps) {

    public Desktop {
        apps = apps == null ? List.of() : List.copyOf(apps);
    }
}
