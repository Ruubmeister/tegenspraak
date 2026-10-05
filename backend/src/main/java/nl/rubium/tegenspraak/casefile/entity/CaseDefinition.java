package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;
import java.util.Optional;
import nl.rubium.tegenspraak.desktop.entity.Desktop;

public record CaseDefinition(
        String id, CaseManifest manifest, List<CharacterProfile> characters, List<Desktop> desktops) {

    public CaseDefinition {
        characters = List.copyOf(characters);
        desktops = List.copyOf(desktops);
    }

    public Optional<CharacterProfile> findCharacter(String characterId) {
        return characters.stream()
                .filter(character -> character.id().equals(characterId))
                .findFirst();
    }

    public Optional<Desktop> findDesktop(String desktopId) {
        return desktops.stream()
                .filter(desktop -> desktop.id().equals(desktopId))
                .findFirst();
    }
}
