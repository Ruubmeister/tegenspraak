package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Virtual desktop of a person or organisation that the player can search for evidence. The id is the file name of the
 * desktop definition and is injected while loading. A lock is optional and hides all apps until the code is entered.
 */
public record Desktop(
        @JacksonInject("id") String id,
        @NotBlank String ownerName,
        @NotNull OwnerType ownerType,
        @NotBlank String description,
        @Valid Lock lock,
        @Valid List<DesktopApp> apps) {

    public Desktop {
        apps = apps == null ? List.of() : List.copyOf(apps);
        UniqueIds.require(apps.stream().map(DesktopApp::id).toList(), "app", id);
    }

    public Optional<DesktopApp> findApp(String appId) {
        return apps.stream().filter(app -> app.id().equals(appId)).findFirst();
    }
}
